# 多阶段构建：用户端 + 管理端 → 一个 nginx 镜像统一入口
FROM node:20-alpine AS user-build
WORKDIR /app
COPY user/package.json user/package-lock.json ./
RUN npm ci
COPY user/ .
RUN npm run build

FROM node:20-alpine AS admin-build
WORKDIR /app
COPY admin/package.json admin/package-lock.json ./
RUN npm ci
COPY admin/ .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=user-build  /app/dist /usr/share/nginx/html/user
COPY --from=admin-build /app/dist /usr/share/nginx/html/admin
EXPOSE 80
