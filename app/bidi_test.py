import sys
from bidi.algorithm import get_display

def test_bidi(text):
    # display prints the visual order from left to right
    # To simulate RTL context, we can prepend an RTL mark (RLM) or use base_dir='R'
    ltr_display = get_display(text, base_dir='L')
    rtl_display = get_display(text, base_dir='R')
    print(f"Original: {text}")
    print(f"Visual LTR Context: {ltr_display}")
    print(f"Visual RTL Context: {rtl_display}")
    print("-" * 40)

text1 = "۲ Oct 2026   ۲۰ صفر ۱۴۴۸"
test_bidi(text1)

text2 = "Oct 2026 ۲   صفر ۱۴۴۸ ۲۰"
test_bidi(text2)
