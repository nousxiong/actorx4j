package actorx.adl;
public class ActorId extends adata.Base  {
  public long axid = 0L;
  public long timestamp = 0L;
  public long id = 0L;
  public ActorId()
  {
    axid = 0L;
    timestamp = 0L;
    id = 0L;
  }
  public long getAxid(){
    return this.axid;
  }
  public void setAxid(long value){
    this.axid = value;
  }
  public long getTimestamp(){
    return this.timestamp;
  }
  public void setTimestamp(long value){
    this.timestamp = value;
  }
  public long getId(){
    return this.id;
  }
  public void setId(long value){
    this.id = value;
  }
  public void read(adata.Stream stream)
  {
    int offset = stream.readLength();
    long tag = stream.readInt64();
    int len_tag = stream.readInt32();

    if((tag&1L)>0)    this.axid = stream.readInt64();
    if((tag&2L)>0)    this.timestamp = stream.readInt64();
    if((tag&4L)>0)    this.id = stream.readInt64();
    if(len_tag >= 0)
    {
      int read_len = (int)(stream.readLength() - offset);
      if(len_tag > read_len) stream.skipRead(len_tag - read_len);
    }
  }
  public void skipRead(adata.Stream stream)
  {
    stream.skipReadCompatible();
  }
  public int sizeOf()
  {
    int size = 0;
    long tag = 7L;
    size += adata.Stream.sizeOfInt64(this.axid);
    size += adata.Stream.sizeOfInt64(this.timestamp);
    size += adata.Stream.sizeOfInt64(this.id);
    size += adata.Stream.sizeOfInt64(tag);
    size += adata.Stream.sizeOfInt32(size + adata.Stream.sizeOfInt32(size));
    return size;
  }
  public void write(adata.Stream stream)
  {
    long tag = 7L;
    stream.writeInt64(tag);
    stream.writeInt32(this.sizeOf());
    stream.writeInt64(this.axid);
    stream.writeInt64(this.timestamp);
    stream.writeInt64(this.id);
  }
  public void rawRead(adata.Stream stream)
  {
    this.axid = stream.readInt64();
    this.timestamp = stream.readInt64();
    this.id = stream.readInt64();
  }
  public int rawSizeOf()
  {
    int size = 0;
    size += adata.Stream.sizeOfInt64(this.axid);
    size += adata.Stream.sizeOfInt64(this.timestamp);
    size += adata.Stream.sizeOfInt64(this.id);
    return size;
  }
  public void rawWrite(adata.Stream stream)
  {
    stream.writeInt64(this.axid);
    stream.writeInt64(this.timestamp);
    stream.writeInt64(this.id);
  }
}

