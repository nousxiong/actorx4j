package actorx.adl;
public class ActorExit extends adata.Base  {
  public byte type = 0;
  public String errmsg = "";
  public ActorExit()
  {
    type = 0;
    errmsg = "";
  }
  public byte getType(){
    return this.type;
  }
  public void setType(byte value){
    this.type = value;
  }
  public String getErrmsg(){
    return this.errmsg;
  }
  public void setErrmsg(String value){
    this.errmsg = value;
  }
  public void read(adata.Stream stream)
  {
    int offset = stream.readLength();
    long tag = stream.readInt64();
    int len_tag = stream.readInt32();

    if((tag&1L)>0)    this.type = stream.readInt8();
    if((tag&2L)>0)    {
      int len2= stream.checkReadSize(0);
      this.errmsg = stream.readString(len2);
    }
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
    long tag = 1L;
    if(this.errmsg.length() > 0){tag|=2L;}
    size += adata.Stream.sizeOfInt8(this.type);
    if((tag&2L)>0)
    {
      size += adata.Stream.sizeOfString(this.errmsg);
    }
    size += adata.Stream.sizeOfInt64(tag);
    size += adata.Stream.sizeOfInt32(size + adata.Stream.sizeOfInt32(size));
    return size;
  }
  public void write(adata.Stream stream)
  {
    long tag = 1L;
    if(this.errmsg.length() > 0){tag|=2L;}
    stream.writeInt64(tag);
    stream.writeInt32(this.sizeOf());
    stream.writeInt8(this.type);
    if((tag&2L)>0)    {
      stream.writeString(this.errmsg,0);
    }
  }
  public void rawRead(adata.Stream stream)
  {
    this.type = stream.readInt8();
    {
      int len2= stream.checkReadSize(0);
      this.errmsg = stream.readString(len2);
    }
  }
  public int rawSizeOf()
  {
    int size = 0;
    size += adata.Stream.sizeOfInt8(this.type);
    {
      size += adata.Stream.sizeOfString(this.errmsg);
    }
    return size;
  }
  public void rawWrite(adata.Stream stream)
  {
    stream.writeInt8(this.type);
    {
      stream.writeString(this.errmsg,0);
    }
  }
}

