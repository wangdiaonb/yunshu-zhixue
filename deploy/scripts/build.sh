#!/usr/bin/env bash
# 一键构建：后端 jar + 两份前端静态产物，统一收集到 deploy/dist
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/deploy/dist"

echo "==> 清理产物目录：${OUT_DIR}"
rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}/portal" "${OUT_DIR}/admin" "${OUT_DIR}/backend"

echo "==> 后端打包"
cd "${ROOT_DIR}/backend"
mvn -B clean package -DskipTests
cp target/yunshu-zhixue-backend.jar "${OUT_DIR}/backend/"

echo "==> 学员端构建"
cd "${ROOT_DIR}/portal-web"
pnpm install --frozen-lockfile
pnpm build
cp -r dist/. "${OUT_DIR}/portal/"

echo "==> 管理端构建"
cd "${ROOT_DIR}/admin-web"
pnpm install --frozen-lockfile
pnpm build
cp -r dist/. "${OUT_DIR}/admin/"

echo "==> 构建完成"
echo "    backend: ${OUT_DIR}/backend/yunshu-zhixue-backend.jar"
echo "    portal : ${OUT_DIR}/portal"
echo "    admin  : ${OUT_DIR}/admin"
