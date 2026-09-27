# 自部署 DropRun

[English](SELF_HOSTING.md) · [发布说明](../releases/0.5.0.md)

当前为公开预览版。重要项目使用前请阅读验证缺口。一套 Relay 对应一个所有者和
一台 Windows Connector，可配对多个手机；没有官方注册账号。

## 准备

- Windows x64、Git、已登录的本地 Codex；Chrome 或 Edge 用于页面验证。
- Android 8 或更新手机。
- 自己的 Cloudflare 账号，启用 Workers、D1、R2。R2 可能要求在官方控制台开通
  订阅。供应商用量可能产生费用，DropRun 不代你购买付费计划。

## 安装与连接

1. 从 [GitHub Releases](https://github.com/MrMaii/droprun/releases) 下载安装包或
   便携 ZIP，对照发布清单校验 SHA-256。Windows 可能提示未知发布者。
2. 安装器按当前用户安装；便携版解压到固定位置后运行 `DropRun.cmd`。
   已包含 Node，数据保存在 `%LOCALAPPDATA%\DropRun`。
3. 本机浏览器向导检查环境，打开 Cloudflare 官方登录，让你选择账号并创建私有
   Relay。所需媒体工具从上游单独下载，保持各自许可。
4. 部署失败后重试原步骤；向导保留进度，不用换实例名重新创建。
5. 启动 Connector，手机安装 APK 后扫码，确认电脑和 HTTPS 服务器地址，再授权
   可交办的项目。配对邀请一次有效且会过期，不要公开真实二维码。
6. 从其他 Android 应用分享材料，选项目并可选留言，观察真实处理与交付状态。

“已保存”只表示手机已可靠保存待发送内容。电脑离线时，已被 Relay 接收的任务会
排队，电脑上线后才执行。独立分享创建新任务；追问复用会话；网络重试不增加次数。

公共版与旧私人/debug 包并行安装，不尝试跨签名覆盖。确认新连接正常前保留旧版。
新配对的执行模式在设置中可见；直接执行与先看计划均保留既有权限边界。

## 预览、隐私与费用

已上传的截图和文件不依赖电脑持续在线。支持的静态导出可以生成独立来源、限时
访问的网页快照；依赖实时后端或特殊资源路径的项目不自动视为静态网站。
实时预览需自己的域名、managed Cloudflare Tunnel 和在线电脑。临时 Tunnel 仅用于开发。

源码免费，Cloudflare、可选转写及 Codex 用量属于你自己的账号。数据去向和清理边界
见[数据与隐私](PRIVACY.md)。云端删除不会撤销原项目已发生的修改。

## 从源码运行

```sh
npm ci
node scripts/setup.mjs doctor
node scripts/setup.mjs open
```

显式命令行部署：

```sh
node scripts/setup.mjs login
node scripts/setup.mjs deploy --account YOUR_ACCOUNT_ID --name droprun-personal --accept-cloud-costs
```

该参数表示知悉供应商用量条款，不会购买无限额度。首次部署使用新基线；旧实例
必须走兼容升级，不能在已有数据库上重复运行建表基线。

## 升级与故障

先阅读发布说明，等待活动任务结束，备份本地数据与实例，再升级。保留相同数据
目录和配对身份，新版健康检查及交办通过后再移除旧程序。Relay 与数据库升级
由所有者显式执行。当前自动化范围与手动步骤以发布说明为准。

材料不可读时，报告必须说明实际取得范围；可改为分享有权使用的直接文件。
诊断仅提供脱敏信息，不上传凭证、配对码或私人项目路径。
