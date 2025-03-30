public class WeaveProcess {
    public static final int MAX_BLOCKS = 1024;
    public Block[] blocks;
    int largestIndex = 0;

    public WeaveProcess() {
        this.blocks = new Block[1024];
    }

    public Block addBlock(int pos) {
        //TOOD(Ray): Get from scheduler
        if (this.largestIndex < pos) {
            this.largestIndex = pos;
        }

        return blocks[pos] = new Block(new StringBuilder());
    }
    // TODO(Ray): Unit Test this function
    public void swapBlocks(int idxA, int idxB) {
        Block temp = blocks[idxA];
        blocks[idxA] = blocks[idxB];
        blocks[idxB] = temp;

        int maxIdx = Math.max(idxA, idxB);
        this.largestIndex = Math.max(maxIdx, largestIndex);
    }

    public Block removeBlockAndShift(int pos) {
        Block removedbBlock = blocks[pos];
        shiftBlocksLeft(pos, largestIndex);
        largestIndex--;
        return removedbBlock;
    }

    public void insertBlockAndShift(int pos, Block block) {
        if (pos > largestIndex) {
            blocks[pos] = block;
            largestIndex = pos;
        } else {
            shiftBlocksRight(pos, largestIndex + 1);
            blocks[pos] = block;
            largestIndex++;
        }
    }

    private void shiftBlocksRight(int startPos, int endPos) {
        for (int i = endPos; i > startPos; i--) {
            blocks[i] = blocks[i - 1];
        }
        blocks[startPos] = null;
    }

    private void shiftBlocksLeft(int startPos, int endPos) {
        for (int i = startPos; i < endPos; i++) {
            blocks[i] = blocks[i + 1];
        }
        blocks[endPos] = null;
    }
}