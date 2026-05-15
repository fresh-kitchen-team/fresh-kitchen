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
export JWT_SECRET=$(aws ssm get-parameter --name "/fresh-kitchen/JWT_SECRET" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export GOOGLE_CLIENT_ID=$(aws ssm get-parameter --name "/fresh-kitchen/GOOGLE_CLIENT_ID" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export KAKAO_CLIENT_ID=$(aws ssm get-parameter --name "/fresh-kitchen/KAKAO_CLIENT_ID" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export REDIS_HOST=$(aws ssm get-parameter --name "/fresh-kitchen/REDIS_HOST" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export GEMINI_API_KEY=$(aws ssm get-parameter --name "/fresh-kitchen/GEMINI_API_KEY" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export AI_SERVER_BASE_URL=$(aws ssm get-parameter --name "/fresh-kitchen/AI_SERVER_BASE_URL" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export AI_SERVER_TOKEN=$(aws ssm get-parameter --name "/fresh-kitchen/AI_SERVER_TOKEN" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export IMAGE_STORAGE_TYPE=$(aws ssm get-parameter --name "/fresh-kitchen/IMAGE_STORAGE_TYPE" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export AWS_S3_BUCKET=$(aws ssm get-parameter --name "/fresh-kitchen/AWS_S3_BUCKET" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export AWS_ACCESS_KEY_ID=$(aws ssm get-parameter --name "/fresh-kitchen/AWS_ACCESS_KEY_ID" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export AWS_SECRET_ACCESS_KEY=$(aws ssm get-parameter --name "/fresh-kitchen/AWS_SECRET_ACCESS_KEY" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)
export IMAGE_STORAGE_S3_PUBLIC_BASE_URL=$(aws ssm get-parameter --name "/fresh-kitchen/IMAGE_STORAGE_S3_PUBLIC_BASE_URL" --with-decryption --query Parameter.Value --output text --region $AWS_REGION)


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