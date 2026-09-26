package com.nutrition.security;

import com.nutrition.config.JwtAuthFilter;
import com.nutrition.enums.JwtRoleEnum;
import com.nutrition.util.JwtUtil;
import com.nutrition.util.RedisCache;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 鉴权过滤器安全边界测试。
 * <p>
 * 覆盖生产上线阻断项：附件接口收紧、管理类接口仅 ADMIN 可访问、
 * 服务间回调与重置密码接口的放行策略。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterSecurityTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisCache redisCache;
    @Mock
    private FilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, redisCache);
    }

    /** 附件上传接口不再免鉴权：无 Token 必须 401。 */
    @Test
    void shouldRequireTokenForAttachmentUpload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/attachment/upload");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    /** 附件删除接口不再免鉴权：无 Token 必须 401。 */
    @Test
    void shouldRequireTokenForAttachmentDelete() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/attachment/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    /**
     * 附件 URL 解析接口必须保持公开只读：
     * 前端 &lt;image&gt; 标签直接引用该地址，无法携带 JWT。
     */
    @Test
    void shouldAllowPublicAttachmentUrl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/attachment/123/url");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    /** 普通用户访问 AI 配置接口必须 403。 */
    @Test
    void shouldForbidUserAccessingAiConfig() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/api/ai/config/list"), "user-token", JwtRoleEnum.USER.getCode());

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /** 普通用户访问 AI 配置新增接口（无子路径）也必须 403。 */
    @Test
    void shouldForbidUserAccessingAiConfigRoot() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("POST", "/api/ai/config"), "user-token", JwtRoleEnum.USER.getCode());

        assertEquals(403, response.getStatus());
    }

    /** 普通用户访问内容审核接口必须 403。 */
    @Test
    void shouldForbidUserAccessingAudit() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/api/audit/pending-review"), "user-token", JwtRoleEnum.USER.getCode());

        assertEquals(403, response.getStatus());
    }

    /** 普通用户访问知识库管理接口必须 403。 */
    @Test
    void shouldForbidUserAccessingKnowledgeBase() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/api/rag/knowledge/list"), "user-token", JwtRoleEnum.USER.getCode());

        assertEquals(403, response.getStatus());
    }

    /** 管理员访问 AI 配置接口应放行。 */
    @Test
    void shouldAllowAdminAccessingAiConfig() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/api/ai/config/list"), "admin-token", JwtRoleEnum.ADMIN.getCode());

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /** 重置密码接口必须免 JWT（未登录用户忘记密码时使用）。 */
    @Test
    void shouldAllowAnonymousResetPassword() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/reset-password");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    /** 知识库回调接口免 JWT（由控制器校验 API Key），不应被 401 拦截。 */
    @Test
    void shouldAllowAnonymousKnowledgeCallback() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/rag/knowledge/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    /** 无 Token 访问受保护接口返回 401。 */
    @Test
    void shouldRejectMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
    }

    // ==================== 接口文档（Swagger/OpenAPI）保护 ====================

    /** Swagger UI 无 Token 必须 401，不能公网匿名访问。 */
    @Test
    void shouldRequireTokenForSwaggerUi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    /** OpenAPI 文档无 Token 必须 401。 */
    @Test
    void shouldRequireTokenForApiDocs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    /** 普通用户即使登录也不能访问 OpenAPI 文档，必须 403。 */
    @Test
    void shouldForbidUserAccessingApiDocs() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/v3/api-docs/swagger-config"), "user-token", JwtRoleEnum.USER.getCode());

        assertEquals(403, response.getStatus());
    }

    /** 管理员访问 OpenAPI 文档应放行。 */
    @Test
    void shouldAllowAdminAccessingApiDocs() throws Exception {
        MockHttpServletResponse response = doFilterWithToken(
                new MockHttpServletRequest("GET", "/v3/api-docs"), "admin-token", JwtRoleEnum.ADMIN.getCode());

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletResponse doFilterWithToken(MockHttpServletRequest request, String token, String role)
            throws Exception {
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisCache.exists(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtUtil.getRoleFromToken(token)).thenReturn(role);

        filter.doFilter(request, response, chain);
        return response;
    }
}
