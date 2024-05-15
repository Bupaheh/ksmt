package io.ksmt.solver.wrapper.bv2int

data class KBv2IntRewriterConfig(
    val rewriteMode: KBv2IntRewriter.RewriteMode = KBv2IntRewriter.RewriteMode.EAGER,
    val andRewriteMode: KBv2IntRewriter.AndRewriteMode = KBv2IntRewriter.AndRewriteMode.SUM,
    val signednessMode: KBv2IntRewriter.SignednessMode = KBv2IntRewriter.SignednessMode.UNSIGNED,
    val disableRewriting: Boolean = false,
    val enableSplitter: Boolean = false,
) {
    val isLazyOverflow: Boolean
        get() = !disableRewriting &&
                (signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW ||
                signednessMode == KBv2IntRewriter.SignednessMode.SIGNED_LAZY_OVERFLOW_NO_BOUNDS)

    val isLazyBvAnd
        get() = !disableRewriting && rewriteMode == KBv2IntRewriter.RewriteMode.LAZY

    override fun toString(): String {
        if (disableRewriting) return "NoRewriting"

        return "$rewriteMode;$signednessMode"
    }
}
