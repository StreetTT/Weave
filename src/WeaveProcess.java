


//represents a single process containing a sequence of code blocks
public class WeaveProcess {
    public static final int MAX_BLOCKS = 1024;
    public Block[] blocks;
    public String name = "";
    int largestIndex = 0;
    public ProcessRow myRow;


    //constructor for a new weave process
    public WeaveProcess() {
        this.blocks = new Block[1024];
    }

    //adds a new, empty block at the specified position
    public Block addBlock(int pos) {
        if (this.largestIndex < pos) {
            this.largestIndex = pos;
        }

        return blocks[pos] = new Block(new StringBuilder());
    }


    //removes the block data at the specified position
    public void removeBlock(int pos) {
        //if removing the block at the current largest index
        if (this.largestIndex == pos)  {
            int newLargestIndex = 0;
            for (int i = 0; i < this.largestIndex; ++i) {
                if (this.blocks[i] != null) {
                    newLargestIndex = i;
                }
            }
            //updates the largest index
            this.largestIndex = newLargestIndex;
        }

        //sets the block at the specified position to null
        this.blocks[pos] = null;
    }

    //duplicates an existing block's data into the specified position
    public Block duplicateBlock(int pos, Block block) {
        return blocks[pos] = block.deepCopy();
    }

    //swaps the block data between two positions
    public void swapBlocks(int idxA, int idxB) {

        //performs the swap using a temporary variable
        Block temp = blocks[idxA];
        blocks[idxA] = blocks[idxB];
        blocks[idxB] = temp;

        //recalculates largest index properly after swap
        int maxIdx = Math.max(idxA, idxB);
        this.largestIndex = Math.max(maxIdx, largestIndex);
    }


    //removes a block and shifts all subsequent blocks one position to the left
    public Block removeBlockAndShift(int pos) {
        //stores the block being removed
        Block removedbBlock = blocks[pos];
        //shifts blocks starting from the position after the removed one
        shiftBlocksLeft(pos, largestIndex);
        //decrements the largest index since the sequence is now shorter
        largestIndex--;
        //returns the block that was removed
        return removedbBlock;
    }


    //inserts a block at a position and shifts subsequent blocks to the right
    public void insertBlockAndShift(int pos, Block block) {
        //if inserting beyond the current largest index, just place it
        if (pos > largestIndex) {
            blocks[pos] = block;
            largestIndex = pos;
            //if inserting within the existing sequence
        } else {
            shiftBlocksRight(pos, largestIndex + 1);
            blocks[pos] = block;
            largestIndex++;
        }
    }


    //helper method to shift blocks to the right within the array
    private void shiftBlocksRight(int startPos, int endPos) {
        //ensure we don't write out of bounds
        for (int i = endPos; i > startPos; i--) {
            //moves block one position right
            blocks[i] = blocks[i - 1];
        }
        blocks[startPos] = null;
    }


    //helper method to shift blocks to the left within the array
    private void shiftBlocksLeft(int startPos, int endPos) {
        //iterates from the start position up to the second to last block
        for (int i = startPos; i < endPos; i++) {
            //moves block one position left
            blocks[i] = blocks[i + 1];
        }
        blocks[endPos] = null;
    }
}