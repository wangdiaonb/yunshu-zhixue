#!/usr/bin/env bash
# 产物发布：把 deploy/dist 同步到目标服务器并重启后端、重载 Nginx
#
# 必填环境变量：
#   DEPLOY_HOST  目标服务器地址
#   DEPLOY_USER  登录用户（需具备 sudo 权限）
# 可选环境变量：
#   DEPLOY_PORT       SSH 端口，默认 22
#   PORTAL_PATH       学员端产物目录，默认 /usr/share/nginx/html/portal
#   ADMIN_PATH        管理端产物目录，默认 /usr/share/nginx/html/admin
#   BACKEND_HOME      后端部署目录，默认 /opt/yunshu-zhixue
#   BACKEND_SERVICE   systemd 服务名，默认 yunshu-zhixue
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIST_DIR="${ROOT_DIR}/deploy/dist"

: "${DEPLOY_HOST:?请先设置 DEPLOY_HOST}"
: "${DEPLOY_USER:?请先设置 DEPLOY_USER}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
PORTAL_PATH="${PORTAL_PATH:-/usr/share/nginx/html/portal}"
ADMIN_PATH="${ADMIN_PATH:-/usr/share/nginx/html/admin}"
BACKEND_HOME="${BACKEND_HOME:-/opt/yunshu-zhixue}"
BACKEND_SERVICE="${BACKEND_SERVICE:-yunshu-zhixue}"

if [[ ! -d "${DIST_DIR}" ]]; then
  echo "未找到构建产物，请先执行 deploy/scripts/build.sh" >&2
  exit 1
fi

SSH_OPTS=(-p "${DEPLOY_PORT}")
REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"

echo "==> 同步前端产物"
rsync -az --delete -e "ssh ${SSH_OPTS[*]}" "${DIST_DIR}/portal/" "${REMOTE}:${PORTAL_PATH}/"
rsync -az --delete -e "ssh ${SSH_OPTS[*]}" "${DIST_DIR}/admin/" "${REMOTE}:${ADMIN_PATH}/"

echo "==> 上传后端 jar"
rsync -az -e "ssh ${SSH_OPTS[*]}" "${DIST_DIR}/backend/yunshu-zhixue-backend.jar" "${REMOTE}:${BACKEND_HOME}/app.jar"

echo "==> 重启后端并重载 Nginx"
ssh "${SSH_OPTS[@]}" "${REMOTE}" "sudo systemctl restart ${BACKEND_SERVICE} && sudo nginx -t && sudo systemctl reload nginx"

echo "==> 发布完成：http://${DEPLOY_HOST}/  与  http://${DEPLOY_HOST}/admin/"
