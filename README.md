# MiaoChat

一个 Minecraft Fabric 客户端模组，利用 AI 大模型将你的聊天消息实时改写为猫娘风格。

## 功能特性

- **AI 猫娘改写**：发送消息前自动调用 AI 将文本改写为可爱的猫娘风格
- **降级改写**: 在使用normal模式下使用正则表达插入喵来改写句子
- **上下文感知**：自动收集游戏内聊天记录作为上下文，让改写更自然
- **多种聊天模式**：支持普通聊天和猫娘模式切换
- **思考模式**：可选启用 AI 思考链（reasoning），提升改写质量
- **Debug 模式**：详细的请求/响应日志，方便调试
- **高度可配置**：支持自定义 API 地址、模型、系统提示词等

## 配置

首次运行后会在 `config/miaochat.json` 生成配置文件：

```json
{
  "api_url": "https://api.openai.com/v1/chat/completions",
  "api_key": "your-api-key",
  "model": "gpt-3.5-turbo",
  "system_prompt": "你是一个可爱的猫娘...",
  "context_size": 15,
  "max_tokens": 2048,
  "thinking": false,
  "debug": false
}
```

| 字段 | 说明 |
|------|------|
| `api_url` | 兼容 OpenAI 格式的 API 地址 |
| `api_key` | API 密钥 |
| `model` | 模型名称 |
| `system_prompt` | 系统提示词 |
| `context_size` | 上下文消息条数 |
| `max_tokens` | 最大响应 token 数 |
| `thinking` | 是否启用模型的思考模式 |
| `debug` | 是否启用调试日志 |

## 使用方法

在游戏中使用 `/miaochat mode <mode>` 命令切换猫娘模式。开启后，你发送的每条消息都会经过 AI 改写。
mode可用的参数:`none` `ai` `normal`
默认值:`none`
## 构建

```bash
# 默认构建 (Minecraft 1.21.4)
./gradlew build

# 指定 Minecraft 版本
./gradlew build -Pminecraft_version=1.21.5 -Pyarn_mappings=1.21.5+build.1 -Pfabric_version=0.120.0+1.21.5
```

构建产物位于 `build/libs/`。

我们推荐你使用Agent构建,这会避免很多问题
## 环境要求

- Minecraft 1.21.4+
- Fabric Loader
- Java 21+

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE.txt) 开源。
