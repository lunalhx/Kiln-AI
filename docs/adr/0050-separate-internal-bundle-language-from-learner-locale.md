---
status: accepted
---

# Separate internal Bundle language from learner locale

First-party Profile and Bundle instructions are authored in English, while `learner_locale` is an explicit Node Context View field that controls every learner-visible task, label, and flow message. Source Original language and learner locale remain distinct provenance fields: translating a task for the learner never replaces or mutates the source record. The reference fixture defaults learner-visible output to `zh-CN`.
