/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression

import org.rust.ide.surroundWith.RsSurrounderTestBase

class RsWithIfExpSurrounderTest : RsSurrounderTestBase(RsWithIfExpSurrounder()) {
    fun `test simple`() = doTest(
        """
        fn main() {
            <selection>true</selection>
        }
    """, """
        fn main() {
            if true {<caret>}
        }
    """
    )

    fun `test call`() = doTest(
        """
        fn func() -> bool {
            false
        }

        fn main() {
            <selection>func()</selection>
        }
    """, """
        fn func() -> bool {
            false
        }

        fn main() {
            if func() {<caret>}
        }
    """
    )

    fun `test correct post process`() = doTest(
        """
        fn main() {
            <selection>true</selection>
            1;
        }
    """, """
        fn main() {
            if true {<caret>}
            1;
        }
    """
    )

    fun `test number`() = doTestNotApplicable(
        """
        fn main() {
            <selection>1234</selection>
        }
    """
    )

    fun `test number call`() = doTestNotApplicable(
        """
        fn func() -> i32 {
            1234
        }

        fn main() {
            <selection>func()</selection>
        }
    """
    )
}
