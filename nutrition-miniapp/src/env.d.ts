/// <reference types="@dcloudio/types" />
/// <reference types="@dcloudio/uni-h5" />

declare const uni: typeof import('@dcloudio/uni-app').default

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
