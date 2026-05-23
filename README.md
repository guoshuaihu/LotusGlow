# 珠宝微信小程序

这是一个基于 `Spring Boot + MySQL/H2 + uni-app Vue 3 + Vue 3 管理后台` 的珠宝电商示范项目，当前重点是完成“品牌展示、商品浏览、购物车下单、支付回调模拟、售后与基础后台运营”的闭环。

## 项目结构

- `backend`：Spring Boot 后端，提供小程序端和后台端 API。
- `miniapp`：基于 `uni-app + Vue 3` 的小程序前端，包含首页、分类、商品详情、购物车和我的页面。
- `admin`：基于 `Vue 3 + Vite` 的运营后台，包含登录、订单处理、商品概览、首页内容预览和定制需求查看。
- `tools`：本地辅助工具，目前包含桌面微信桥接脚本。

## 当前能力

### 后端

- 小程序模拟微信登录、手机号绑定和登录态刷新。
- 首页内容聚合、Banner、分类和商品推荐。
- 商品 SPU/SKU、商品详情、库存、收藏、购物车和地址管理。
- 创建订单、发起支付、支付回调幂等处理、后台发货、确认收货。
- 售后申请、后台审核、退款状态联动。
- 后台基础 RBAC、商品运营、库存调整和操作审计。

### 小程序端

- 新中式珠宝风格首页：品牌首屏、分类入口、热卖商品、活动专题、轻定制入口和服务背书。
- 分类搜索：按分类和关键词筛选商品。
- 商品详情：图片、标签、价格、SKU 库存、轻定制备注、证书材质和配送售后说明。
- 购物车结算：规格确认、地址录入、订单提交和支付前校验提示。
- 我的页面：订单、收藏、地址、售后记录和手机号绑定入口。

### 后台端

- 管理员登录。
- 商品列表、分类新增、商品新增、SKU 库存调整。
- 订单发货。
- 售后审核与退款记录。
- 首页 Banner 和内容块查看。

## 启动方式

### 1. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

默认使用 H2 内存库，便于本地快速体验。

如果切换 MySQL：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

需要提前配置环境变量：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

### 2. 启动小程序 H5 预览

```powershell
cd miniapp
npm install
npm run dev:h5
```

接口地址在 [env.ts](./miniapp/src/config/env.ts) 中配置，默认是 `http://127.0.0.1:8080`。

### 3. 启动后台

```powershell
cd admin
npm install
npm run dev
```

默认后台账号：

- 账号：`admin`
- 密码：`Admin@123`

## 验证命令

```powershell
cd backend
.\mvnw.cmd test

cd ..\miniapp
npm run type-check
npm run build:h5

cd ..\admin
npm run build
```

## 本次重构重点

- 修复小程序前端和种子数据里的中文乱码。
- 参考新中式珠宝小程序常见设计，重构前台视觉和信息架构。
- 抽出价格、订单状态、售后状态格式化工具。
- 强化详情页规格库存、轻定制、证书材质和售后说明。
- 改进微信桌面桥接脚本的 DPI 和窗口置前能力，便于后续继续做竞品观察。
