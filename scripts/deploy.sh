#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# CodeDeploy로 배포 시 .env는 deploy.sh와 같은 scripts/ 디렉토리에 위치
if [ ! -f "$DIR/.env" ]; then
  echo "ERROR: .env 파일을 찾을 수 없습니다: $DIR/.env" >&2
  exit 1
fi
source "$DIR/.env"

echo "▶ ECR 로그인"
aws ecr get-login-password --region "${AWS_REGION:-us-east-1}" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "▶ 기존 컨테이너 종료"
# 배포 번들 구조: scripts/deploy.sh, scripts/docker-compose.yml, scripts/.env 가 동일 디렉토리에 위치
docker-compose -f "$DIR/docker-compose.yml" down || true

echo "▶ 새 컨테이너 실행"
docker-compose -f "$DIR/docker-compose.yml" up -d

echo "✅ 배포 완료"