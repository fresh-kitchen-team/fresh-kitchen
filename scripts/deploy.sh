#!/bin/bash
set -e


export ECR_REGISTRY="339713026502.dkr.ecr.us-east-1.amazonaws.com"
export ECR_REPOSITORY="fresh-kitchen"
export IMAGE_TAG="latest"
export AWS_REGION="us-east-1"

# Parameter Store에서 값 가져오기
echo "▶ Parameter Store에서 환경변수 로드"
export DB_URL=$(aws ssm get-parameter --name "/fresh-kitchen/DB_URL" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export DB_USERNAME=$(aws ssm get-parameter --name "/fresh-kitchen/DB_USERNAME" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export DB_PASSWORD=$(aws ssm get-parameter --name "/fresh-kitchen/DB_PASSWORD" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)

export S3_ACCESS_KEY=$(aws ssm get-parameter --name "S3_ACCESS_KEY" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export S3_SECRET_KEY=$(aws ssm get-parameter --name "S3_SECRET_KEY" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export S3_BUCKET_NAME=$(aws ssm get-parameter --name "S3_BUCKET_NAME" --query Parameter.Value --output text --region $AWS_REGION)

echo "------------------ 서버 배포 시작 --------------------------------"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2. .env 파일 체크 (파일이 없어도 배포가 중단되지 않도록 수정)
if [ -f "$DIR/.env" ]; then
  echo "▶ .env 파일을 로드합니다."
  source "$DIR/.env"
else
  echo "⚠️ .env 파일이 없지만, 설정된 환경 변수로 계속 진행합니다."
fi

echo "▶ ECR 로그인"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "▶ 기존 컨테이너 종료"
# docker-compose가 설치되어 있어야 합니다.
docker-compose -f "$DIR/docker-compose.yml" down || true

echo "▶ 새 컨테이너 실행"
docker-compose -f "$DIR/docker-compose.yml" up -d

echo "✅ 배포 완료"