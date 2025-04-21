public class Block {
    StringBuilder fileContents;
    public Block(StringBuilder contents) {
        fileContents = contents;
    }

    public Block deepCopy() {
        return new Block(new StringBuilder(this.fileContents));
    }
}
