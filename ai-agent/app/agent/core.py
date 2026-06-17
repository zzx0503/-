import re
import json

from langchain_openai import ChatOpenAI
from langchain.agents import AgentExecutor
from langchain.agents.format_scratchpad import format_log_to_str
from langchain.agents.output_parsers import ReActSingleInputOutputParser
from langchain.tools.render import render_text_description

from app.config import settings
from app.agent.tools import TOOLS
from app.agent.prompts import CHAT_SYSTEM_PROMPT


def get_llm():
    return ChatOpenAI(
        model=settings.model_name,
        openai_api_base=settings.openai_api_base,
        openai_api_key=settings.openai_api_key,
        temperature=settings.temperature,
        max_tokens=settings.max_tokens,
    )


def get_agent_executor(system_prompt: str = None):
    llm = get_llm().bind(stop=["\nObservation"])

    tools_str = render_text_description(TOOLS)
    tool_names_str = ", ".join(t.name for t in TOOLS)
    sys = system_prompt or CHAT_SYSTEM_PROMPT

    def build_prompt(inputs: dict) -> str:
        scratchpad = format_log_to_str(inputs.get("intermediate_steps", []))
        return f"""{sys}

工具:
{tools_str}

可用工具名: {tool_names_str}

使用以下格式回复：
Question: 用户的问题
Thought: 思考下一步做什么
Action: 工具名称（必须从"可用工具名"中选择一个）
Action Input: 工具参数（JSON格式）
Observation: 工具返回的结果
...（重复 Thought/Action/Action Input/Observation 直到获取足够信息）
Thought: 可以给出最终答案
Final Answer: 最终回复给用户的内容

重要规则：
- 一次只能调用一个工具，拿到结果后再决定下一步
- Action Input 必须是有效的 JSON 字符串

Question: {inputs["input"]}
{scratchpad}"""

    def parse_output(text: str):
        final_match = re.search(r"Final Answer:\s*(.*)", text, re.DOTALL)
        if final_match:
            from langchain_core.agents import AgentFinish
            return AgentFinish({"output": final_match.group(1).strip()}, text)

        action_match = re.search(
            r"Action:\s*(.+?)\s*\n\s*Action Input:\s*(.+?)(?:\n|$)",
            text, re.DOTALL
        )
        if action_match:
            from langchain_core.agents import AgentAction
            tool_name = action_match.group(1).strip()
            tool_input = action_match.group(2).strip()
            try:
                tool_input = json.loads(tool_input)
            except json.JSONDecodeError:
                pass
            return AgentAction(tool=tool_name, tool_input=tool_input, log=text)

        from langchain_core.agents import AgentFinish
        return AgentFinish({"output": text}, text)

    from langchain_core.runnables import RunnableLambda, RunnablePassthrough

    agent = (
        RunnablePassthrough.assign(
            agent_scratchpad=lambda x: format_log_to_str(x["intermediate_steps"]),
        )
        | RunnableLambda(build_prompt)
        | llm
        | RunnableLambda(lambda msg: parse_output(msg.content))
    )

    return AgentExecutor(
        agent=agent, tools=TOOLS,
        verbose=False, handle_parsing_errors=True,
        max_iterations=3, return_intermediate_steps=True,
    )
