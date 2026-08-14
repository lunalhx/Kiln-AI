---
status: superseded
---

# Use Spring AI per-call tools from the authorized set

> Superseded by the destructive Apply cutover (ticket 06): the calculator,
> Tool Resolver, Tool Session, and tool budget were removed. The Apply stack
> is zero-tool by contract (every reference Bundle declares `tools: []`), and
> `ApplyModelAdapter` never attaches tool callbacks. Per-call tools may return
> when a later Profile needs them.

Teaching nodes will use Spring AI ChatClient tool calling instead of a second application protocol. The authorized set is still produced by the application ToolResolver from the frozen Skill Stack and Teaching Node Profile; the adapter maps only those ToolHandles onto this request via ChatClient `.tools(...)`. Shared ChatClient beans must not register `defaultTools`. Pedagogy, Assessment, Input Interpreter, and format repair receive no tools. Tool executions consume Tool Budget and stop that wake-up when the ceiling is reached. Domain ports stay free of Spring AI types.
