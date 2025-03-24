package dev.buildcli.core.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AIConstantsTest {

	@Test
	void testDocumentCodePrompt() {
		String expectedPrompt = """
				### Strict Code Commenting Prompt

				You are an AI assistant specialized in code documentation. Your task is to add explanatory comments to the given code WITHOUT modifying anything else.

				🚨 STRICT RULES – DO NOT BREAK THESE RULES! 🚨
				1. DO NOT modify, remove, or change any part of the original code.
				   - ❌ Do not remove imports, packages, classes, methods, or variables.
				   - ❌ Do not make methods abstract or modify their implementation.
				   - ❌ Do not add, rename, or remove parameters.
				   - ❌ Do not reformat or restructure the code.
				2. DO NOT suggest improvements, refactorings, or changes.
				3. DO NOT rewrite or reorganize anything—ONLY ADD COMMENTS.
				4. Preserve all original formatting, spacing, and indentation.

				### How to Add Comments:
				✔️ Inline comments (`//` for Java, JavaScript, C#, etc., `#` for Python) should be placed next to important lines of code.
				✔️ Block comments (`/** ... */` or `""\" ... ""\"`) should be used to document classes and functions.
				✔️ Describe the intent behind the code and complex logic—avoid redundant or obvious comments.
				✔️ Only add comments—do not rewrite, replace, or remove anything.

				---

				### Example Before:
				```java
				package com.example;

				import com.example.AClass;

				public class AClass {
				  public int add(int a, int b) {
				      return a + b;
				  }
				}
				```

				### Example After (ONLY ADDING COMMENTS, NOTHING ELSE):
				```java
				package com.example;

				import com.example.AClass;

				public class AClass {
				  /**
				   * Adds two integers and returns the result.
				   * @param a The first integer.
				   * @param b The second integer.
				   * @return The sum of 'a' and 'b'.
				   */
				  public int add(int a, int b) {
				      return a + b; // Returns the sum of the two input values.
				  }
				}
				```

				---

				### Now, add comments to the following code WITHOUT MODIFYING ANYTHING:
				""";
		assertEquals(AIConstants.DOCUMENT_CODE_PROMPT, expectedPrompt);
	}

	@Test
	void testCommentCodePrompt() {
		String expectedPrompt = """
				*"Review the following code for readability, performance, maintainability, and best practices. Identify potential bugs, security vulnerabilities, and areas for optimization. Suggest improvements while keeping the code’s intended functionality intact. Provide clear explanations for each suggestion."* \s

				Your comments should be **precise**, explaining not just what is wrong but why it is an issue and how it can be improved."* \s
				""";
		assertEquals(AIConstants.COMMENT_CODE_PROMPT, expectedPrompt);
	}

	@Test
	void testGenerateTestPrompt() {
		String expectedPrompt = """
				You are a software testing expert, and your task is to generate automated tests in %s. \
				The tests must follow the AAA methodology (Arrange, Act, Assert) and ensure at least 80%% code coverage \
				(both line and case coverage), including edge cases and invalid inputs.

				For each test:

				    Arrange: Set up the test environment, initialize necessary data, and create required objects.
				    Act: Execute the function or method under test.
				    Assert: Verify that the results match expectations, covering both positive and negative scenarios.

				The tests should be well-structured, readable, and follow best practices for the chosen language and testing framework. \
				Ensure clear descriptions for each test case and include boundary conditions, edge cases, and error handling scenarios.
				""";
		assertEquals(AIConstants.GENERATE_TEST_PROMPT, expectedPrompt);
	}
}
