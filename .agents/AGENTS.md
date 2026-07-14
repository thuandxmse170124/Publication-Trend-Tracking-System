
<RULE[user_global]>
# Strict Branch Segregation Rule
Whenever modifying files, agents MUST verify that the files belong to the current feature branch's scope. Do NOT mix changes belonging to multiple feature branches (e.g., feature/sync, feature/publication-trend, feature/research-paper) into a single branch. If changes span multiple scopes, use 'git stash' and 'git checkout' to isolate and commit them into their respective branches. Never commit everything into one branch if it violates branch responsibilities.
</RULE[user_global]>
