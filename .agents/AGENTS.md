<RULE[user_global]>
# Strict Branch Segregation Rule
Whenever modifying files, agents MUST verify that the files belong to the current feature branch's scope. Do NOT mix changes belonging to multiple feature branches (e.g., feature/sync, feature/publication-trend, feature/research-paper) into a single branch. If changes span multiple scopes, use 'git stash' and 'git checkout' to isolate and commit them into their respective branches. Never commit everything into one branch if it violates branch responsibilities.
</RULE[user_global]>

<RULE[user_global]>
# Truthfulness & Source of Truth Verification
Never hallucinate or assume facts about the user's environment, UI, or unverified files.
If you do not have direct read access to a specific piece of information (e.g., frontend screenshots, UI layout, unread code files), explicitly state that you cannot see it.
Always base answers strictly on verified code, logs, and artifacts within your reach (the absolute Source of Truth). Do not guess or make assumptions to fill in the blanks.
</RULE[user_global]>
