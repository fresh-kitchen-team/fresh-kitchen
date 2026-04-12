#!/bin/bash
set -e

echo "▶ ECR 로그인"
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin 339713026502.dkr.ecr.us-east-1.amazonaws.com

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "▶ 기존 컨테이너 종료"
docker-compose -f "$DIR/docker-compose.yml" down || true
docker container prune -f

echo "▶ 새 컨테이너 실행"
docker-compose -f "$DIR/docker-compose.yml" up -d

echo "✅ 배포 완료"