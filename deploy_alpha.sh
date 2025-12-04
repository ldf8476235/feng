#!/bin/bash

# ================= 配置信息 =================
HOST="198.46.175.142"
USER="root"
PASS="57zVTrYhkK9cv9L4E6"
REMOTE_DIR="/usr/local/lee"
JAR_NAME="alpha-0.0.1-SNAPSHOT.jar"
LOCAL_JAR_PATH="alpha/target/$JAR_NAME"
# ===========================================

# 1. 检查本地文件是否存在
if [ ! -f "$LOCAL_JAR_PATH" ]; then
  echo "❌ [Error] 找不到文件: $LOCAL_JAR_PATH"
  exit 1
fi

echo "📤 [1/2] 正在上传 Jar 包到服务器..."

sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$LOCAL_JAR_PATH" $USER@$HOST:$REMOTE_DIR/

if [ $? -ne 0 ]; then
  echo "❌ [Error] 上传失败！"
  exit 1
fi

echo "🔄 [2/2] 正在远程重启服务..."

sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no $USER@$HOST "cd $REMOTE_DIR && chmod +x restart_alpha.sh && ./restart_alpha.sh"

echo "✅ 部署完成！"
