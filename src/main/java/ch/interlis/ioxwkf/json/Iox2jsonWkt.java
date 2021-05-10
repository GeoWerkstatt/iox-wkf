package ch.interlis.ioxwkf.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTWriter;

import ch.ehi.ili2db.json.Iox2json;
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
import ch.interlis.iox_j.jts.Iox2jts;
import ch.interlis.iox_j.jts.Iox2jtsException;

class Iox2jsonWkt extends Iox2json {
    protected WKTWriter wktWriter=null;
    public Iox2jsonWkt(JsonGenerator jg,TransferDescription td) throws IOException
    {
        super(jg,td);
        wktWriter=new WKTWriter();
    }
    @Override
    protected void writeCoordAttr(ch.interlis.iom.IomObject obj, 
            String propName) throws IOException {
        IomObject geomStruct=obj.getattrobj(propName,0);
        String geomTxt=null;
        if(geomStruct!=null) {
            Coordinate geom;
            try {
                geom = Iox2jts.coord2JTS(geomStruct);
            } catch (Iox2jtsException e) {
                throw new IOException(e);
            }
            geomTxt=WKTWriter.toPoint(geom);
        }
        if(geomTxt!=null) {
            jg.writeStringField(propName,geomTxt);
        }
    }
    @Override
    protected void writeLineAttr(ch.interlis.iom.IomObject obj,
            String propName,LineType type) throws IOException {
        IomObject geomStruct=obj.getattrobj(propName,0);
        String geomTxt=null;
        if(geomStruct!=null) {
            LineString geom;
            try {
                geom = Iox2jts.polyline2JTSlineString(geomStruct,false,type.getP());
            } catch (Iox2jtsException e) {
                throw new IOException(e);
            }
            geomTxt=wktWriter.write(geom);
        }
        if(geomTxt!=null) {
            jg.writeStringField(propName,geomTxt);
        }
    }
    @Override
    protected void writeSurfaceAttr(ch.interlis.iom.IomObject obj, 
            String propName,AbstractSurfaceOrAreaType type) throws IOException {
        IomObject geomStruct=obj.getattrobj(propName,0);
        String geomTxt=null;
        if(geomStruct!=null) {
            Polygon geom;
            try {
                geom = Iox2jts.surface2JTS(geomStruct, type.getP());
            } catch (Iox2jtsException e) {
                throw new IOException(e);
            }
            geomTxt=wktWriter.write(geom);
        }
        if(geomTxt!=null) {
            jg.writeStringField(propName,geomTxt);
        }
    }
}
