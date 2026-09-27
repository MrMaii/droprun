# ADR 0012：让用户看得见效果

日期：2026-09-09  
状态：已采纳，随 0.2.1 上线

## 背景

用户描述的目标场景：刷到一个前端效果视频 → 转发给个人网站项目并留言换到某一页 → 继续刷 → Codex 直接开工 → 报告里说「这个效果叫什么、我改了哪页」，附最终效果截图和一个可点开的预览链接。0.2.0 能让 Codex「知道要做什么」，但 Codex 看不清 10 秒一帧的短视频、报告没有截图、localhost 的项目手机进不去。

## 决定

1. **密集抽帧 + 联络表。** `samplingPlan(duration)`：≤ 40 秒每秒一帧，否则最多 40 帧均匀覆盖（上限 `maxVideoMinutes`，默认 30 分钟）；帧按时间顺序拼成 4×5 联络表（最多 3 张）附给 Codex，再附最多 8 张场景切换帧；转写按 5 分钟分段。给模型的图片总数仍限 12 张。
2. **截图由 Connector 完成。** Codex 在沙箱里调用 `GET 127.0.0.1:47493/screenshot?url=<本机地址>`，Connector 用无头 Chrome（DevTools 协议，独立 profile）截图，保存在任务材料目录，并作为「可信产物」上传（不经证据块声明；证据规则仍只管项目文件）。手机内嵌显示图片类产物。
3. **预览由 Connector 托管。** `POST /preview`：Connector 在项目目录启动白名单内的 dev 命令（package.json 的 dev/start/preview/serve/storybook/docs 脚本、项目内静态目录、`python -m http.server`），前置一个只认随机钥匙（首次带 `k=` 访问后换成 cookie）的反向代理，用 cloudflared 临时隧道暴露，`previewMinutes`（默认 30）后自动关闭；同一项目开始新任务时先关旧预览。链接写入任务的 `preview_url/preview_expires_at`，手机显示「打开预览」。不在沙箱内让 Codex 自己起服务器，避免命令和端口失控。
4. **登录态用独立浏览器 profile。** `GET /login` 打开 Connector 自己的 Chrome profile 让用户登录一次；下载前用 DevTools `Network.getAllCookies` 导出 Netscape cookies 给 yt-dlp。不读用户日常浏览器的 cookies（Chrome 应用绑定加密使其不可读，也不该读）。
5. **Relay 自补列。** `POST /connector/migrate` 只添加缺失列，Connector 启动时调用，避免每次改表都要手工操作线上库。
6. **对抗外部进程清理。** Codex 桌面应用每 5 分钟按进程名终止所有 `codex.exe`。Connector 运行同目录的 `codex-droprun.exe` 副本；目录会话断开自动重开；执行回合被打断在同一线程续跑一次（保留已记录的事件，证据核对仍以真实执行记录为准）；连接层错误自动重试；退出时清理预览与隧道。

## 影响

- Connector 新依赖：`cloudflared`（npm 包自带二进制）、本机 Chrome/Edge（无则截图与登录功能自动关闭，健康接口 `screenshots/previews/cookies` 字段可查）。
- 预览链接是短期公网地址，安全边界是随机钥匙 + 有效期；报告和手机里都会带钥匙，不应对外转发。
- 开发者指令新增「让用户看得见效果」一节，Codex 对页面/界面改动默认执行预览 + 截图；接口报错时要写进「未完成」而不是伪造。
