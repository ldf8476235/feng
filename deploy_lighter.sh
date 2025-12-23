#!/bin/bash

# ================= 配置信息 =================
HOST="198.46.175.142"
USER="root"
PASS="Wsldf981126"
REMOTE_DIR="/usr/local/lee"
JAR_NAME="lighter-0.0.1-SNAPSHOT.jar"
LOCAL_JAR_PATH="lighter/target/$JAR_NAME"
# ===========================================

# 1. Maven 打包
echo "📦 [1/3] 正在执行 Maven 打包..."

mvn -s /usr/local/maven/conf/settings.xml package -f lighter/pom.xml -DskipTests

if [ $? -ne 0 ]; then
  echo "❌ [Error] Maven 打包失败！"
  exit 1
fi

echo "✅ Maven 打包成功"

# 2. 检查本地文件是否存在
if [ ! -f "$LOCAL_JAR_PATH" ]; then
  echo "❌ [Error] 找不到文件: $LOCAL_JAR_PATH"
  exit 1
fi

echo "📤 [2/3] 正在上传 Jar 包到服务器..."

sshpass -p "$PASS" scp -o StrictHostKeyChecking=no "$LOCAL_JAR_PATH" $USER@$HOST:$REMOTE_DIR/

if [ $? -ne 0 ]; then
  echo "❌ [Error] 上传失败！"
  exit 1
fi

echo "🔄 [3/3] 正在远程重启服务..."

sshpass -p "$PASS" ssh -o StrictHostKeyChecking=no $USER@$HOST "cd $REMOTE_DIR && chmod +x restart_lighter.sh && ./restart_lighter.sh"

echo "✅ 部署完成！"
