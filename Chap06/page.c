#include <stdio.h>

#define PAGE_SHIFT 12
#define PAGE_SIZE (1UL << PAGE_SHIFT)
/* WARNING: DO NOT EDIT, AUTO-GENERATED CODE - SEE TOP FOR INSTRUCTIONS */
#define PAGE_MASK (~(PAGE_SIZE-1))

#define PAGE_START(x)  ((x) & PAGE_MASK)

// Returns the offset of address 'x' in its page.
#define PAGE_OFFSET(x) ((x) & ~PAGE_MASK)

// Returns the address of the next page after address 'x', unless 'x' is
// itself at the start of a page.
#define PAGE_END(x)    PAGE_START((x) + (PAGE_SIZE-1))

#define ADDRESS 0x1020

int main(){
    printf("PAGE_MASK:0x%x\n",PAGE_MASK);
    printf("PAGE_START:0x%x\n",PAGE_START(ADDRESS));
    printf("PAGE_END:0x%x\n",PAGE_END(ADDRESS));
    printf("PAGE_OFFSET:0x%x\n",PAGE_OFFSET(ADDRESS));
    return 0;
}