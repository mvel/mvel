mule-mvel
=========

This is a fork from [MVEL](https://github.com/mvel/mvel/) moved to a different
package. As MVEL uses some global configuration settings, this was needed so
that Mule Expression Language (powered by mvel) didn't break other libraries
also relying on mvel, but with a different global config.



MVEL's master branch is tracked in the upstream-master branch, and nothing is commited there.
This branch has upstream changes up to MVEL 2.1.8.Final

The following customizations have been added to this fork:
- Packages have been renamed, and package/class references by name updated



---
Important: This fork is not intended for general use. It will not be supported if used outside Mule ESB.
---
