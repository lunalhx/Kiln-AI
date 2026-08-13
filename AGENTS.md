# AGENTS.md

## 规则 (原版, side project 适用)

- Do not preserve backward compatibility. Remove obsolete paths instead of adding compatibility layers, fallbacks, or migrations.
  （不保留向后兼容。删除过时的代码路径，而不是添加兼容层、fallback 或 migration。）

- Choose the simplest implementation that fully meets the current requirements. Avoid speculative abstractions, configuration, and indirection.
  （选择完全满足当前需求的最简单实现。避免投机性的抽象、多余的配置和间接层。）

- Grow the system in layers. Start from the smallest version that works end to end, and add each new capability on top of a product that already works. Never trade a working product for unfinished complexity.
  （分层构建系统。从端到端可工作的最小版本开始，在已经可工作的产品之上逐层添加新能力。绝不用尚未完成的复杂度去换一个可用的产品。）

- Keep components modular and concerns clearly separated.
  （保持组件模块化，关注点清晰分离。）

- Prefer established, well-maintained libraries when they reduce overall complexity or improve reliability. Do not reimplement common functionality without a clear reason.
  （当成熟、维护良好的库能降低整体复杂度或提升可靠性时优先使用。没有明确理由，不要重新实现常见功能。）

- Lean on the dependencies already in the project before writing your own implementation or adding packages. Do not assume a library lacks a capability without checking its documentation and types.
  （先依靠项目已有的依赖，再自己实现或添加新 package。不查看库的文档和类型之前，不要假设它缺少某个能力。）

- Make architectural decisions for the long term. Do not accept a stopgap that only works for now and is meant to be replaced later.
  （架构决策要往长远做。不接受「只能现在用、以后要换」的权宜方案。）

- Study how established products solve the problem before designing a solution. Adopt their proven patterns and conventions rather than inventing an approach from scratch.
  （设计方案之前，先研究成熟产品如何解决这个问题。采用它们经过验证的 pattern 和约定，而不是从头发明。）
