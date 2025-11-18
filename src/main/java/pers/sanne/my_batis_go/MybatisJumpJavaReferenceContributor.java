package pers.sanne.my_batis_go;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import pers.sanne.my_batis_go.references.JavaOperatorReference;
import pers.sanne.my_batis_go.utils.CommonUtil;

public class MybatisJumpJavaReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(XmlAttributeValue.class),
                new XmlIdReferenceProvider()
        );
    }

    /**
     * Provider for MyBatis XML id → Java method reference.
     */
    public static class XmlIdReferenceProvider extends PsiReferenceProvider {

        @Override
        public PsiReference @NotNull [] getReferencesByElement(
                @NotNull PsiElement element,
                @NotNull ProcessingContext context
        ) {

            if (!(element instanceof XmlAttributeValue xmlAttributeValue)) {
                return PsiReference.EMPTY_ARRAY;
            }

            // <select id="xxx"> ... </select>
            XmlTag parentTag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
            if (parentTag == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            // check if tag name is MyBatis operator: select / insert / update / delete
            if (!CommonUtil.containMybatisOperator(parentTag.getName())) {
                return PsiReference.EMPTY_ARRAY;
            }

            // ensure attribute is `id="something"`
            PsiElement parent = xmlAttributeValue.getParent();
            if (!(parent instanceof XmlAttribute attribute)) {
                return PsiReference.EMPTY_ARRAY;
            }
            if (!"id".equals(attribute.getName())) {
                return PsiReference.EMPTY_ARRAY;
            }

            // ensure XML is under /src/main/resources/mybatis
            Project project = xmlAttributeValue.getProject();
            String mybatisPath = project.getBasePath() + "/src/main/resources/mybatis";

            PsiFile containingFile = xmlAttributeValue.getContainingFile();
            VirtualFile virtualFile = containingFile.getVirtualFile();

            if (virtualFile == null || !virtualFile.getPath().contains(mybatisPath)) {
                return PsiReference.EMPTY_ARRAY;
            }

            // ensure it's an XML file
            if (!(containingFile instanceof XmlFile xmlFile)) {
                return PsiReference.EMPTY_ARRAY;
            }

            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag == null) {
                return PsiReference.EMPTY_ARRAY;
            }

            // must have namespace="com.xxx.xxxMapper"
            String namespace = rootTag.getAttributeValue("namespace");
            if (namespace == null || namespace.isEmpty()) {
                return PsiReference.EMPTY_ARRAY;
            }

            // id value
            String queryId = xmlAttributeValue.getValue();

            // create reference
            return new PsiReference[]{
                    new JavaOperatorReference(xmlAttributeValue, namespace, queryId)
            };
        }
    }
}
