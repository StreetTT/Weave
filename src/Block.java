

//represents a single block of code within a weave process
public class Block {
    //holds the actual code content for this block
    StringBuilder fileContents;
    //constructor to create a block with initial content
    public Block(StringBuilder contents) {
        fileContents = contents;
    }

    //creates a completely independent copy of this block and its content
    public Block deepCopy()
    {
        //creates a new block with a new stringbuilder containing the same text
        return new Block(new StringBuilder(this.fileContents));
    }
}
