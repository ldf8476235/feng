#!/bin/bash

# ================= 配置区域 =================
# 只需填写 Jar 包的文件名
APP_NAME="your-app.jar"

# 日志文件名
LOG_FILE="app.log"
# ===========================================

# 关键点：获取脚本所在的绝对路径，并进入该目录
# 这样无论你在服务器的哪个地方执行这个脚本，它都能找到旁边的 jar 包
cd $(dirname $0)

echo "当前目录: $(pwd)"
echo "准备重启 $APP_NAME ..."

# 1. 查找并杀掉旧进程
# 使用 ps -ef 查找包含 APP_NAME 的进程
pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -n "$pid" ]; then
  echo "发现正在运行的进程 (PID: $pid)，正在停止..."
  kill -9 $pid
  sleep 2 # 等待进程结束
  echo "旧进程已停止。"
else
  echo "未发现正在运行的进程，直接启动。"
fi

# 2. 启动新进程
echo "正在启动新进程..."

# nohup 后台启动
nohup java -jar $APP_NAME > $LOG_FILE 2>&1 &

# 3. 检查是否启动成功
new_pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -n "$new_pid" ]; then
    echo "========================================"
    echo "启动成功！"
    echo "新 PID: $new_pid"
    echo "日志文件: $(pwd)/$LOG_FILE"
    echo "========================================"
else
    echo "启动失败，请检查日志。"
fi