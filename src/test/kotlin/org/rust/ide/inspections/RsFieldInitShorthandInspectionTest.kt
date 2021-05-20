/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections

class RsFieldInitShorthandInspectionTest : RsInspectionsTestBase(RsFieldInitShorthandInspection::class) {

    fun `test not applicable`() = checkFixIsUnavailable(
        "Use initialization shorthand", """
        fn main() {
            let _ = S { foo: bar/*caret*/, baz: &baz };
        }
    """, checkWeakWarn = true
    )

    fun `test fix`() = checkFixByText(
        "Use initialization shorthand", """
        fn main() {
            let _ = S { <weak_warning descr="Expression can be simplified">foo: foo/*caret*/</weak_warning>, baz: quux };
        }
    """, """
        fn main() {
            let _ = S { foo/*caret*/, baz: quux };
        }
    """, checkWeakWarn = true
    )
}
