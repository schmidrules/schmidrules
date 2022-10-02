package org.schmidrules.xmi;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.schmidrules.configuration.dto.ComponentDto;
import org.schmidrules.configuration.dto.PackageReferenceDto;

public class CreationPhase {
    
    private static final Namespace xmiNs = Namespace.getNamespace("xmi", "https://www.omg.org/spec/XMI/2.1.1");
    private static final Namespace umlNs = Namespace.getNamespace("uml", "https://www.omg.org/spec/UML/2.1.2");

    private final String projectName;
    private final Collection<PreparedComponent> components;

    public CreationPhase(String projectName, Collection<PreparedComponent> components) {
        this.projectName = projectName;
        this.components = components;
    }

    public void output(OutputStream out) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(createDocument(), out);
    }

    private Document createDocument() {
        Document doc = new Document();

        Element xmiRootElement = new Element("XMI", xmiNs);
        xmiRootElement.setAttribute("version", "2.1", xmiNs);
        xmiRootElement.addNamespaceDeclaration(umlNs);
        doc.addContent(xmiRootElement);

        Element xmiDocumentation = new Element("Documentation", xmiNs);
        xmiDocumentation.setAttribute("exporter", "Enterprise Architect");
        xmiDocumentation.setAttribute("exporterVersion", "6.5");
        xmiRootElement.addContent(xmiDocumentation);

        Element umlModel = new Element("Model", umlNs);
        umlModel.setAttribute("type", "uml:Model", xmiNs);
        umlModel.setAttribute("name", "EA_Model");
        umlModel.setAttribute("visibility", "public");
        xmiRootElement.addContent(umlModel);

        String umlPackageId = IdGenerator.createId();
        Element umlPackage = new Element("packagedElement");
        umlPackage.setAttribute("type", "uml:Package", xmiNs);
        umlPackage.setAttribute("id", umlPackageId, xmiNs);
        umlPackage.setAttribute("name", projectName);
        umlPackage.setAttribute("visibility", "public");
        umlModel.addContent(umlPackage);

        List<Element> umlClasses = new ArrayList<>();
        List<Element> umlDependencies = new ArrayList<>();
        // for the documentation
        List<Element> extensionElements = new ArrayList<>();
        // for the geometry (position and size)
        List<Element> extensionDiagramElements = new ArrayList<>();

        int seqNo = 1;

        for (PreparedComponent preparedComponent : components) {
            umlClasses.add(createUmlClass(preparedComponent));
            umlDependencies.addAll(createUmlDependencies(preparedComponent));
            extensionElements.add(createExtensionElement(preparedComponent));
            extensionDiagramElements.add(createExtensionDiagramElement(preparedComponent, seqNo));

            seqNo++;
        }

        umlPackage.addContent(umlClasses);
        umlPackage.addContent(umlDependencies);

        Element xmiExtension = new Element("Extension", xmiNs);
        xmiExtension.setAttribute("extender", "Enterprise Architect");
        xmiExtension.setAttribute("extenderID", "6.5");
        xmiRootElement.addContent(xmiExtension);

        Element elements = new Element("elements");
        elements.addContent(extensionElements);
        xmiExtension.addContent(elements);

        Element diagrams = new Element("diagrams");
        Element diagram = new Element("diagram");
        diagram.setAttribute("id", IdGenerator.createId(), xmiNs);
        diagrams.addContent(diagram);

        addDiagramContent(diagram, umlPackageId);

        Element diagramElements = new Element("elements");
        diagramElements.addContent(extensionDiagramElements);
        diagram.addContent(diagramElements);

        xmiExtension.addContent(diagrams);

        return doc;
    }

    private static Element createUmlClass(PreparedComponent preparedComponent) {
        ComponentDto component = preparedComponent.getComponent();

        Element umlClass = new Element("packagedElement");
        umlClass.setAttribute("type", "uml:Class", xmiNs);
        umlClass.setAttribute("id", preparedComponent.getXmiId(), xmiNs);
        umlClass.setAttribute("name", component.getId());
        umlClass.setAttribute("visibility", "public");

        if (component.getPackageDependencies() != null) {
            for (PackageReferenceDto packageDependency : component.getPackageDependencies()) {
                Element property = createProperty(packageDependency.getName(), "package", "EAJava_packageDepedency");
                umlClass.addContent(property);
            }
        }

        if (component.getPublicPackages() != null) {
            for (PackageReferenceDto publicPackage : component.getPublicPackages()) {
                Element property = createProperty(publicPackage.getName(), "public", "EAJava_publicPackage");
                umlClass.addContent(property);
            }
        }

        if (component.getInternalPackages() != null) {
            for (PackageReferenceDto internalPackage : component.getInternalPackages()) {
                Element property = createProperty(internalPackage.getName(), "private", "EAJava_internalPackage");
                umlClass.addContent(property);
            }
        }
        return umlClass;
    }

    private static Element createProperty(String name, String visibility, String typeRef) {
        Element property = new Element("ownedAttribute");
        property.setAttribute("type", "uml:Property", xmiNs);
        property.setAttribute("id", IdGenerator.createId(), xmiNs);
        property.setAttribute("name", name);
        property.setAttribute("visibility", visibility);
        property.setAttribute("isStatic", "false");
        property.setAttribute("isReadOnly", "false");
        property.setAttribute("isDerived", "false");
        property.setAttribute("isOrdered", "false");
        property.setAttribute("isUnique", "true");
        property.setAttribute("isDerivedUnion", "false");

        Element type = new Element("type");
        type.setAttribute("idref", typeRef, xmiNs);
        property.addContent(type);
        return property;
    }

    private static List<Element> createUmlDependencies(PreparedComponent preparedComponent) {
        // <packagedElement xmi:type="uml:Dependency"
        // xmi:id="EAID_896AD567_0590_4281_A7AD_CBB573CD644A" visibility="public"
        // supplier="EAID_50883F5D_7E5D_48fd_9507_70F9289B7C2D"
        // client="EAID_36179B72_29CE_42b9_9FEF_EBE6C3F97BD4"/>
        List<Element> umlDependecies = new ArrayList<>();

        for (PreparedComponent dependency : preparedComponent.getPreparedComponentDependencies()) {
            Element umlDependency = new Element("packagedElement");
            umlDependency.setAttribute("type", "uml:Dependency", xmiNs);
            umlDependency.setAttribute("id", IdGenerator.createId(), xmiNs);
            umlDependency.setAttribute("visibility", "public");
            umlDependency.setAttribute("supplier", dependency.getXmiId());
            umlDependency.setAttribute("client", preparedComponent.getXmiId());
            umlDependecies.add(umlDependency);
        }
        return umlDependecies;
    }

    private static Element createExtensionElement(PreparedComponent preparedComponent) {
        ComponentDto component = preparedComponent.getComponent();

        Element classElement = new Element("element");
        classElement.setAttribute("idref", preparedComponent.getXmiId(), xmiNs);
        classElement.setAttribute("type", "uml:Class", xmiNs);
        classElement.setAttribute("name", component.getId());
        classElement.setAttribute("scope", "public");

        Element properties = new Element("properties");
        String documentation;
        if (component.getDescription() == null || component.getDescription().isEmpty()) {
            documentation = "TODO Add documentation";
        } else {
            documentation = component.getDescription().trim().replaceAll("\n", "").replaceAll("\t", "");
        }

        properties.setAttribute("documentation", documentation);
        properties.setAttribute("isSpecification", "false");
        properties.setAttribute("sType", "Class");
        properties.setAttribute("nType", "0");
        properties.setAttribute("scope", "public");
        properties.setAttribute("isRoot", "false");
        properties.setAttribute("isAbstract", "false");
        properties.setAttribute("isActive", "false");

        classElement.addContent(properties);
        return classElement;
    }

    private static Element createExtensionDiagramElement(PreparedComponent preparedComponent, int seqNo) {
        Element classElement = new Element("element");

        int height = 150;
        int top = 160 * seqNo;
        int bottom = top + height;

        StringBuilder geometry = new StringBuilder();
        geometry.append("Left=130;Top=").append(top).append(";");
        geometry.append("Right=269;Bottom=").append(bottom).append(";");

        classElement.setAttribute("geometry", geometry.toString()); // "Left=130;Top=100;Right=269;Bottom=250;"
        classElement.setAttribute("subject", preparedComponent.getXmiId());

        classElement.setAttribute("seqno", String.valueOf(seqNo));
        classElement.setAttribute("style", "DUID=69FBF26C;Notes=1000;");
        return classElement;
    }

    private static void addDiagramContent(Element diagram, String umlPackageId) {
        Element model = new Element("model");
        model.setAttribute("package", umlPackageId);
        model.setAttribute("localID", "9"); // TODO what is this for?
        model.setAttribute("owner", umlPackageId);
        diagram.addContent(model);

        Element properties = new Element("properties");
        properties.setAttribute("name", "Components");
        properties.setAttribute("type", "Component");
        diagram.addContent(properties);

        Element project = new Element("project");
        project.setAttribute("author", "ogrof");
        project.setAttribute("version", "1.0");
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
        project.setAttribute("created", formattedDate);
        project.setAttribute("modified", formattedDate);
        diagram.addContent(project);

        Element style1 = new Element("style1");
        style1.setAttribute("value",
                "ShowPrivate=1;ShowProtected=1;ShowPublic=1;HideRelationships=0;Locked=0;Border=1;HighlightForeign=1;PackageContents=1;SequenceNotes=0;ScalePrintImage=0;PPgs.cx=1;PPgs.cy=1;DocSize.cx=791;DocSize.cy=1134;ShowDetails=0;Orientation=P;Zoom=100;ShowTags=0;OpParams=1;VisibleAttributeDetail=0;ShowOpRetType=1;ShowIcons=1;CollabNums=0;HideProps=0;ShowReqs=0;ShowCons=0;PaperSize=9;HideParents=0;UseAlias=0;HideAtts=0;HideOps=0;HideStereo=0;HideElemStereo=0;ShowTests=0;ShowMaint=0;ConnectorNotation=UML 2.1;ExplicitNavigability=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;ShowNotes=0;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;");
        diagram.addContent(style1);

        Element style2 = new Element("style2");
        style2.setAttribute("value",
                "ExcludeRTF=0;DocAll=0;HideQuals=0;AttPkg=1;ShowTests=0;ShowMaint=0;SuppressFOC=1;MatrixActive=0;SwimlanesActive=1;MatrixLineWidth=1;MatrixLocked=0;TConnectorNotation=UML 2.1;TExplicitNavigability=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;ProfileData=;MDGDgm=;STBLDgm=;ShowNotes=0;VisibleAttributeDetail=0;ShowOpRetType=1;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;SuppressedCompartments=,;SaveTag=7780A26D;");
        diagram.addContent(style2);

        Element swimlanes = new Element("swimlanes");
        swimlanes.setAttribute("value", "locked=false;orientation=0;width=0;inbar=false;names=false;color=0;bold=false;fcol=0;;cls=0;");
        diagram.addContent(swimlanes);

        Element matrixitems = new Element("matrixitems");
        matrixitems.setAttribute("value", "locked=false;matrixactive=false;swimlanesactive=true;width=1;");
        diagram.addContent(matrixitems);

        diagram.addContent(new Element("extendedProperties"));
    }

}
