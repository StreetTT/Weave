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

    public void swapBlocks(int pos1, int pos2) {
        StringBuilder tempContents = blocks[pos1].fileContents;
        blocks[pos1].fileContents = blocks[pos2].fileContents;
    }

    public void removeBlock(int pos) {
        blocks[pos] = null;
    }
}
