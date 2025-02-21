package org.buildcli.constants;

public abstract class AIConstants {
  public static final String DOCUMENT_CODE_PROMPT = """
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

  public static final String COMMENT_CODE_PROMPT = """
      *"Review the following code for readability, performance, maintainability, and best practices. Identify potential bugs, security vulnerabilities, and areas for optimization. Suggest improvements while keeping the code’s intended functionality intact. Provide clear explanations for each suggestion."* \s

      Your comments should be **precise**, explaining not just what is wrong but why it is an issue and how it can be improved."* \s
      """;
}
