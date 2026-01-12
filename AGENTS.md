# Code Style Preferences

## General Rules
- Do not add code comments unless explicitly requested
- Keep code clean and self-documenting
- Remove unnecessary explanatory comments
- All imports must be at the beginning of the file

## Communication Style
- Be concise and direct
- Minimize output tokens while maintaining helpfulness
- No unnecessary preamble or postamble
- Brief confirmations after completing tasks

## File Operations
- Always read files before editing
- Use specialized tools (Read, Edit, Write) instead of bash commands for file operations
- Prefer parallel tool calls when operations are independent

## Issue Tracking

Use `bd` (beads) for all task tracking. **Never use markdown TODOs or read .beads/ directly.**

### Quick Reference

```bash
# Find ready work
bd ready

# Create issues
bd create "Title" -t task -p 2 -d "Description"
bd create "Title" -t epic -p 1                       # Epic: bd-xxx
bd create "Title" -t task -p 2 --parent     # Task: bd-xxx.1
bd create "Title" -t task -p 2 --parent .1  # Sub:  bd-xxx.1.1

# Update status
bd update  --status in_progress
bd update  --notes "Done: X. Remaining: Y"

# Link discovered work
bd dep add   --type discovered-from

# Complete work
bd close  --reason "Done: summary"

# Show dependencies
bd dep tree 

# Get details
bd show 
```

### Hierarchical IDs

```
bd-xxx           [epic]
bd-xxx.1         [task under epic]
bd-xxx.1.1       [sub-task]
bd-xxx.2         [task under epic]
```

### Workflow: Research/Planning

Use when asked to plan, research, or create tasks.

1. **Understand scope**: Clarify requirements, break down into epics/tasks
2. **Create epic**: `bd create "Epic title" -t epic -p 1 -d "Description"`
3. **Create tasks under epic**: 
   ```bash
   bd create "Task 1" -t task -p 2 --parent <epic-id>
   bd create "Task 2" -t task -p 2 --parent <epic-id>
   ```
4. **Add implementation details**: Include actual code snippets/changes in task descriptions
5. **Research dependencies**: Web search for latest library versions when adding libs
6. **Sync**: `bd sync`

**Planning rules:**
- Every code change = explicit task
- Group related changes in one task (one domain = one task)
- Include concrete implementation details in descriptions
- Always web search for current lib versions before specifying dependencies

### Workflow: Implementation

Use when explicitly asked to implement tasks.

1. **Find work**: `bd ready`
2. **Claim task**: `bd update <id> --status in_progress`
3. **Implement**: Code, test, document
4. **Discover work**: Create and link unexpected findings
   ```bash
   bd create "Bug: ..." -t bug -p 1
   bd dep add <new-id> <current-id> --type discovered-from
   ```
5. **Complete**: `bd close <id> --reason "Implemented"`
6. **Sync**: `bd sync`
7. **Next task**: `bd ready` → repeat from step 2

### Issue Types

- `bug` — Defect
- `task` — Work item
- `feature` — New functionality
- `epic` — Large work container
- `chore` — Maintenance

### Priorities

- `0` — Critical
- `1` — High
- `2` — Medium (default)
- `3` — Low
- `4` — Backlog

### Dependency Types

- `blocks` — Hard dependency (affects ready queue)
- `parent-child` — Epic/subtask relationship
- `discovered-from` — Issues found during work
- `related` — Soft relationship

### Rules

- **`bd ready` is your compass** — always start here for implementation
- One task `in_progress` at a time
- Use `--parent` for planned work under epics
- Use `discovered-from` for unexpected findings
- One domain = one task (group related items)

## Questions?

- Check existing issues: `bd list`
- Look at recent commits: `git log --oneline -20`
- Read the docs: README.md, TEXT_FORMATS.md, EXTENDING.md
- Create an issue if unsure: `bd create "Question: ..." -t task -p 2`

## Pro Tips for Agents

- Always use `--json` flags for programmatic use
- Link discoveries with `discovered-from` to maintain context
- Check `bd ready` before asking "what next?"
- Export to JSONL before committing (or use git hooks)
- Use `bd dep tree` to understand complex dependencies
- Priority 0-1 issues are usually more important than 2-4