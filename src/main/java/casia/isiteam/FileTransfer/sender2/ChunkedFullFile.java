package casia.isiteam.FileTransfer.sender2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

public class ChunkedFullFile implements ChunkedInput<ByteBuf> {

	static final int DEFAULT_CHUNK_SIZE = 8192;
	
	boolean firstTime=true;
	 
	private File fileInfo;
    private final RandomAccessFile file;
    private final long startOffset;
    private final long endOffset;
    private final int chunkSize;
    private long offset;

    /**
     * Creates a new instance that fetches data from the specified file.
     */
    public ChunkedFullFile(File file) throws IOException {
        this(file, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #readChunk(ChannelHandlerContext)} call
     */
    public ChunkedFullFile(File file, int chunkSize) throws IOException {
        this(new RandomAccessFile(file, "r"), chunkSize);
        this.fileInfo=file;

    }

    /**
     * Creates a new instance that fetches data from the specified file.
     */
    protected ChunkedFullFile(RandomAccessFile file) throws IOException {
        this(file, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #readChunk(ChannelHandlerContext)} call
     */
    protected ChunkedFullFile(RandomAccessFile file, int chunkSize) throws IOException {
        this(file, 0, file.length(), chunkSize);
    }

    /**
     * Creates a new instance that fetches data from the specified file.
     *
     * @param offset the offset of the file where the transfer begins
     * @param length the number of bytes to transfer
     * @param chunkSize the number of bytes to fetch on each
     *                  {@link #readChunk(ChannelHandlerContext)} call
     */
    protected ChunkedFullFile(RandomAccessFile file, long offset, long length, int chunkSize) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "offset: " + offset + " (expected: 0 or greater)");
        }
        if (length < 0) {
            throw new IllegalArgumentException(
                    "length: " + length + " (expected: 0 or greater)");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "chunkSize: " + chunkSize +
                    " (expected: a positive integer)");
        }

        this.file = file;
        this.offset = startOffset = offset;
        endOffset = offset + length;
        this.chunkSize = chunkSize;

        file.seek(offset);
    }

    /**
     * Returns the offset in the file where the transfer began.
     */
    public long startOffset() {
        return startOffset;
    }

    /**
     * Returns the offset in the file where the transfer will end.
     */
    public long endOffset() {
        return endOffset;
    }

    /**
     * Returns the offset in the file where the transfer is happening currently.
     */
    public long currentOffset() {
        return offset;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return !(offset < endOffset && file.getChannel().isOpen());
    }

    public void close() throws Exception {
        file.close();
    }
    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
           	    
        long offset = this.offset;
        if (offset >= endOffset) {
            return null;
        }
 
       	ByteBuf buf = new UnpooledByteBufAllocator(false).heapBuffer(this.chunkSize);
        
        int chunkSize = (int) Math.min(this.chunkSize, endOffset - offset);
        
    	if(firstTime){   
     		
    		// [int型文件名长度][文件名][long型的文件大小]
    		byte[] bytes=fileInfo.getName().getBytes();
    		int totalSize= Integer.BYTES + bytes.length + Long.BYTES;
    		  		
    		buf.writeInt(bytes.length);
    		buf.writeBytes(bytes);
    		buf.writeLong(fileInfo.length());  
    		    		    		
    		chunkSize = chunkSize- totalSize;
    		firstTime=false;
    	}             
        
        boolean release = true;
        try {        	

            file.readFully(buf.array(), buf.writerIndex(), chunkSize);
            buf.writerIndex(buf.writerIndex()+chunkSize);
            this.offset = offset + chunkSize;
            release = false;
            
            System.out.println(buf.readableBytes());
            return buf;
        }catch(Exception e){
        	e.printStackTrace();
        	
        	throw e;
        }
        finally {
        	firstTime=true;
            if (release) {
                buf.release();
            }
        }
    }

    @Override
    public long length() {
        return endOffset - startOffset;
    }
    @Override
    public long progress() {
        return offset - startOffset;
    }

}
