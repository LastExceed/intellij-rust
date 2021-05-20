/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search

class RsUsageViewTreeTest : RsUsageViewTreeTestBase() {

    fun `test grouping function usages`() = doTestByText(
        """
        fn foo() {}
          //^

        fn bar() {
            foo();
        }

        fn baz() {
            foo();
        }
    """, """
        Usage (2 usages)
         Function
          foo
         Found usages (2 usages)
          function call (2 usages)
           main.rs (2 usages)
            6foo();
            10foo();
    """
    )

    fun `test grouping struct usages`() = doTestByText(
        """
        struct S {
             //^
            a: usize,
        }

        impl S {}
        impl S {}

        fn foo(s1: &S) {}

        fn bar() {
            let s1 = S { a: 1 };
            let a = 1;
            let s2 = S { a };
        }
    """, """
        Usage (5 usages)
         Struct
          S
         Found usages (5 usages)
          impl (2 usages)
           main.rs (2 usages)
            7impl S {}
            8impl S {}
          init struct (2 usages)
           main.rs (2 usages)
            13let s1 = S { a: 1 };
            15let s2 = S { a };
          type reference (1 usage)
           main.rs (1 usage)
            10fn foo(s1: &S) {}
    """
    )
}
