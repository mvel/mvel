# Security Policy

## Security Model

MVEL is an expression language engine designed to **compile and execute code
provided by trusted expression authors**. MVEL expressions are functionally
equivalent to Java code and have the same capabilities, including file system
access, network access, and arbitrary code execution.

**This is by design.** Like other expression language engines (MVEL2, Apache
Commons JEXL, Groovy, Spring Expression Language), MVEL compiles and executes
code. Some of these projects offer optional sandboxing (e.g., JEXL's
`JexlSandbox`, SpEL's `SimpleEvaluationContext`), but none guarantee safe
execution of untrusted input.

**Threat model: MVEL assumes all expression authors are trusted. Executing
untrusted expressions is not a supported use case.**

### For Application Developers

If you embed MVEL in your application:

- **Never pass untrusted user input directly to the MVEL compiler** (for example,
  via `new MVELCompiler().compile(...)`, `new MVEL().compile(...)`, or any
  other compilation entry point).
- The responsibility for restricting who can author expressions lies with your
  application, not with MVEL.
- Treat MVEL expressions with the same level of trust as Java source code.
- If untrusted input must be processed, validate and sanitize it before it
  reaches any MVEL compilation entry point, or run MVEL in an isolated
  environment (e.g., a sandboxed JVM or container).

### What Qualifies as a Vulnerability

We consider the following to be valid security reports:

- Bugs that allow **bypassing a configured security mechanism** (e.g., a class
  allowlist/blocklist, if such a feature is implemented)
- Vulnerabilities in MVEL's **build or distribution infrastructure**
- Bugs that cause MVEL to behave **contrary to its documented security model**

### What Does NOT Qualify as a Vulnerability

The following are **not** considered vulnerabilities, as they reflect intended
functionality:

- Ability to execute arbitrary code via MVEL expressions
- Access to JDK APIs (file I/O, networking, reflection) from expressions
- Remote code execution when untrusted input is passed to the compiler without
  sanitization by the consuming application

## Reporting a Vulnerability

If you discover a security vulnerability in MVEL, please report it through
**GitHub's private vulnerability reporting**:

1. Go to the [Security Advisories page](https://github.com/mvel/mvel/security/advisories/new)
2. Fill in the details and submit

This ensures the report is handled privately and responsibly. Please do **not**
open a public GitHub issue for security vulnerabilities.

### Disclosure Policy

Accepted vulnerabilities will be disclosed via GitHub Security Advisories. We
will coordinate disclosure timing with the reporter. Reporters will be credited
unless they request anonymity.

## Supported Versions

MVEL3 is currently in alpha (3.0.0-SNAPSHOT) and has not had a stable release.
Security reports are still welcome and will be evaluated on a case-by-case basis.

Once stable releases are published, this section will list which versions receive
security fixes. During the alpha phase, only the `main` branch HEAD is
considered supported.
