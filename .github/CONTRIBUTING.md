# Contribution

Contributions are welcome! Feel free to open **Issues** and submit **Pull Requests**.

# How to Contribute

### 1\. Check for an Existing Issue (Required Before Starting Work)

Before you begin working on an issue, ensure that it is not already assigned or actively worked on by another contributor. This helps prevent duplicate efforts and frustration.

#### Steps to Follow

- Check the issue tracker to see if the issue is assigned.

- If the issue is unassigned or recently assigned, comment on it with:

    ```
    Is anyone actively working on this? If not, I'd like to take it up.
    ```

- Wait for confirmation from maintainers or the assigned contributor before proceeding.

If you're unsure about whether you should work on an issue, especially if the assignment is recent, ask a maintainer for clarification.

### 2\. Fork the Project

- Click the **Fork** button on the repository page to create a copy of the repository under your GitHub account.

### 3\. Create a Branch for Your Changes

- Create a new branch to work on your feature or bug fix:

    ```
    git checkout -b feature/my-feature
    ```

### 4\. Commit Your Changes

- Write clear commit messages and follow the project's commit guidelines:

    ```
    git commit -m "My new feature"
    ```

### 5\. Push to Your Branch

- Push your changes to your forked repository:

    ```
    git push origin feature/my-feature
    ```

### 6\. Open a Pull Request (PR)

- Navigate to the main repository and create a **New Pull Request**, ensuring the target branch is `develop`.

- Provide a clear description of the changes, referencing the issue number if applicable.

- (Optional but Recommended) Use the PR template available at `.github/PULL_REQUEST_TEMPLATE/default.md` to structure your PR effectively.

Close Issue on PR Merge
-----------------------

To ensure that issues are automatically closed when a related PR is merged, include one of the following keywords in your PR description:

- `Closes #ISSUE_NUMBER`

- `Fixes #ISSUE_NUMBER`

### Automation Workflow

A GitHub Actions workflow will automatically close the linked issue upon PR merge. For more details checkout `.github\workflows\close-issue-on-pr-merge.yaml`.

Project Environment
-------------------

The project is based on **JDK 21**. Please ensure that you use JDK 21 for development and contribution.

Communication & Issue Handling
------------------------------

### 1\. Avoiding Duplicate Work

To ensure a smooth contribution process and prevent multiple contributors from unknowingly working on the same issue:

- Always check if the issue is assigned before starting.

- If an issue is not explicitly assigned but has recent activity, comment and confirm before proceeding.

- If you find multiple people interested in the same issue, collaborate by discussing a potential division of tasks.

### 2\. Asking for Clarifications

If you have doubts regarding issue ownership, implementation details, or contribution guidelines:

- Leave a comment on the issue or PR.

- Reach out to the maintainers for guidance.

Maintaining Visibility of Contribution Guidelines
-------------------------------------------------

- This contribution guide will be pinned as an issue for visibility.

- The repository's README will include a brief mention of these guidelines with a link to this document.

- A checklist will be included in new issues to remind contributors to verify issue assignments.

By following these guidelines, we aim to foster a more collaborative, efficient, and positive experience for all contributors.