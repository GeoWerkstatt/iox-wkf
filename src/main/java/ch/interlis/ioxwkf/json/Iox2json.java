package ch.interlis.ioxwkf.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import ch.interlis.ili2c.metamodel.AbstractSurfaceOrAreaType;
import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.CoordType;
import ch.interlis.ili2c.metamodel.EnumerationType;
import ch.interlis.ili2c.metamodel.LineType;
import ch.interlis.ili2c.metamodel.MultiPolylineType;
import ch.interlis.ili2c.metamodel.NumericType;
import ch.interlis.ili2c.metamodel.ObjectType;
import ch.interlis.ili2c.metamodel.PolylineType;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.ili2c.metamodel.ViewableTransferElement;
import ch.interlis.iom.IomConstants;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;

class Iox2json {
    private static final String REF = "@ref";
    private static final String REFBID = "@refbid";
    private static final String ORDERPOS = "@orderpos";
    private static final String CONSISTENCY = "@consistency";
    private static final String OPERATION = "@operation";
    private static final String TID = "@id";
    private static final String TYPE = "@type";
    private static final String BID = "@bid";
    private static final String TOPIC = "@topic";
    private static final String CONSISTENCY_ADAPTED = "ADAPTED";
    private static final String CONSISTENCY_INCOMPLETE = "INCOMPLETE";
    private static final String CONSISTENCY_INCONSISTENT = "INCONSISTENT";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";
    
    protected JsonGenerator jg=null;
    protected TransferDescription td=null;
    public Iox2json(JsonGenerator jg,TransferDescription td) throws IOException
    {
        this.jg=jg;
        this.td=td;
    }
    public void write(ch.interlis.iom.IomObject objs[]) throws IOException
    {
        if(objs.length==1) {
            ch.ehi.ili2db.json.Iox2json.writeRaw(jg,objs[0],null);
        }else {
            jg.writeStartArray();
            for(IomObject obj:objs) {
                ch.ehi.ili2db.json.Iox2json.writeRaw(jg,obj,null);
            }
            jg.writeEndArray();
        }
    }
    public void write(ch.interlis.iom.IomObject obj) throws IOException
    {
        write(obj,null,null);
    }
    public void write(ch.interlis.iom.IomObject obj,String bid,String topic) throws IOException
    {
        jg.writeStartObject();
        String className=obj.getobjecttag();
        jg.writeStringField(TYPE,className);
        String oid=obj.getobjectoid();
        if(oid!=null){
            jg.writeStringField(TID,oid);
        }
        if(bid!=null && bid.length()>0){
            jg.writeStringField(BID,bid);
        }
        if(topic!=null && topic.length()>0){
            jg.writeStringField(TOPIC,topic);
        }
        int op = obj.getobjectoperation();
        if(op==IomConstants.IOM_OP_DELETE) {
            jg.writeStringField(OPERATION,OPERATION_DELETE);
        }else if(op==IomConstants.IOM_OP_UPDATE) {
            jg.writeStringField(OPERATION,OPERATION_UPDATE);
        }
        int consistency = obj.getobjectconsistency();
        if(consistency==IomConstants.IOM_INCONSISTENT) {
            jg.writeStringField(CONSISTENCY,CONSISTENCY_INCONSISTENT);
        }else if(consistency==IomConstants.IOM_INCOMPLETE) {
            jg.writeStringField(CONSISTENCY,CONSISTENCY_INCOMPLETE);
        }else if(consistency==IomConstants.IOM_ADAPTED) {
            jg.writeStringField(CONSISTENCY,CONSISTENCY_ADAPTED);
        }
        long orderpos = obj.getobjectreforderpos();
        if(orderpos!=0) {
            jg.writeNumberField(ORDERPOS,orderpos);
        }
        String refbid = obj.getobjectrefbid();
        if(refbid!=null){
            jg.writeStringField(REFBID,refbid);
        }
        String refoid = obj.getobjectrefoid();
        if(refoid!=null){
            jg.writeStringField(REF,refoid);
        }
        
        Viewable aclass=null;
        if(td!=null) {
            aclass=(Viewable) td.getElement(className);
        }
        if(aclass!=null) {
            Iterator attri=aclass.getAttributesAndRoles2();
            while(attri.hasNext()) {
                ViewableTransferElement propDef = (ViewableTransferElement) attri.next();
                if (propDef.obj instanceof AttributeDef) {
                    AttributeDef attr = (AttributeDef) propDef.obj;
                    if(!attr.isTransient()){
                        Type proxyType=attr.getDomain();
                        if(proxyType!=null && (proxyType instanceof ObjectType)){
                            // skip implicit particles (base-viewables) of views
                        }else{
                            String propName=attr.getName();
                            if(attr.isDomainBoolean()) {
                                writeBooleanAttr(obj, propName);
                            }else{
                                Type type=attr.getDomainResolvingAll();
                                if(type instanceof CompositionType) {
                                    writeStructAttr( obj,  propName);
                                }else if(type instanceof NumericType) {
                                    writeNumericAttr( obj, propName);
                                }else if(type instanceof CoordType) {
                                    writeCoordAttr( obj, propName);
                                }else if(type instanceof AbstractSurfaceOrAreaType) {
                                    writeSurfaceAttr( obj, propName,(AbstractSurfaceOrAreaType) type);
                                }else if(type instanceof PolylineType || type instanceof MultiPolylineType) {
                                    writeLineAttr(obj, propName,(LineType)type);
                                }else {
                                    writeStringAttr( obj, propName);
                                }
                            }
                        }
                    }
                }else if(propDef.obj instanceof RoleDef){
                    RoleDef role = (RoleDef) propDef.obj;
                    AssociationDef roleOwner = (AssociationDef) role.getContainer();
                    if (roleOwner.getDerivedFrom() == null) {
                        String propName=role.getName();
                        writeStructAttr(obj, propName);
                    }
                }else {
                    throw new IllegalStateException("unexpected property "+propDef.obj);
                }
            }
        }else {
            boolean isNumeric=false;
            if(className.equals("COORD") || className.equals("ARC")) {
                isNumeric=true;
            }
            int attrc = obj.getattrcount();
            String propNames[]=new String[attrc];
            for(int i=0;i<attrc;i++){
                   propNames[i]=obj.getattrname(i);
            }
            java.util.Arrays.sort(propNames);
            for(int i=0;i<attrc;i++){
               String propName=propNames[i];
                int propc=obj.getattrvaluecount(propName);
                if(propc>0){
                    jg.writeFieldName(propName);
                    if(propc>1){
                        jg.writeStartArray();
                    }
                    for(int propi=0;propi<propc;propi++){
                        String value=obj.getattrprim(propName,propi);
                        if(value!=null){
                            if(isNumeric) {
                                jg.writeNumber(value);
                            }else {
                                jg.writeString(value);
                            }
                        }else{
                            IomObject structvalue=obj.getattrobj(propName,propi);
                            write(structvalue);
                        }
                    }
                    if(propc>1){
                        jg.writeEndArray();
                    }
                }
            }
            
        }
        jg.writeEndObject();
    }
    protected void writeStringAttr(ch.interlis.iom.IomObject obj, String propName)
            throws IOException {
        String value=obj.getattrprim(propName,0);
        if(value!=null){
            jg.writeStringField(propName,value);
        }
    }
    protected void writeCoordAttr(ch.interlis.iom.IomObject obj, 
            String propName) throws IOException {
        IomObject structvalue=obj.getattrobj(propName,0);
        if(structvalue!=null) {
            jg.writeFieldName(propName);
            write(structvalue);
        }
    }
    protected void writeLineAttr(ch.interlis.iom.IomObject obj,
            String propName,LineType type) throws IOException {
        IomObject structvalue=obj.getattrobj(propName,0);
        if(structvalue!=null) {
            jg.writeFieldName(propName);
            write(structvalue);
        }
    }
    protected void writeSurfaceAttr(ch.interlis.iom.IomObject obj, 
            String propName,AbstractSurfaceOrAreaType type) throws IOException {
        IomObject structvalue=obj.getattrobj(propName,0);
        if(structvalue!=null) {
            jg.writeFieldName(propName);
            write(structvalue);
        }
    }
    protected void writeNumericAttr(ch.interlis.iom.IomObject obj, String propName)
            throws IOException {
        String value=obj.getattrprim(propName,0);
        if(value!=null){
            jg.writeFieldName(propName);
            jg.writeNumber(value);
        }
    }
    protected void writeStructAttr(ch.interlis.iom.IomObject obj,
            String propName) throws IOException {
        int propc=obj.getattrvaluecount(propName);
        if(propc>0){
            jg.writeFieldName(propName);
            if(propc>1){
                jg.writeStartArray();
            }
            for(int propi=0;propi<propc;propi++){
                IomObject structvalue=obj.getattrobj(propName,propi);
                write(structvalue);
            }
            if(propc>1){
                jg.writeEndArray();
            }
        }
    }
    protected void writeBooleanAttr(ch.interlis.iom.IomObject obj, String propName)
            throws IOException {
        String value=obj.getattrprim(propName,0);
        if(value!=null){
            if(value.equals("true")) {
                jg.writeBooleanField(propName,true);
            }else {
                jg.writeBooleanField(propName,false);
            }
        }
    }
}
