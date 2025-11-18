package pers.sanne.my_batis_go.references;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.sanne.my_batis_go.utils.CommonUtil;

public class MyBatisXmlReference extends PsiReferenceBase<PsiLiteralExpression> {
    private static final Logger logger = Logger.getInstance(MyBatisXmlReference.class);
    private final String namespace;
    private final String queryId;

    public MyBatisXmlReference(@NotNull PsiLiteralExpression element, @NotNull String namespace, @NotNull String queryId) {
        super(element, new TextRange(1, element.getTextLength() - 1));
        this.namespace = namespace;
        this.queryId = queryId;
    }

    public @Nullable PsiElement resolve() {
        Project project = ((PsiLiteralExpression)this.myElement).getProject();

        for(VirtualFile vFile : this.getVirtualFiles(project)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile instanceof XmlFile xmlFile) {
                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null && this.namespace.equals(rootTag.getAttributeValue("namespace"))) {
                    for(XmlTag tag : rootTag.getSubTags()) {
                        if (CommonUtil.containMybatisOperator(tag.getName()) && this.queryId.equals(tag.getAttributeValue("id"))) {
                            return tag;
                        }
                    }
                }
            }
        }

        return null;
    }

    public PsiElement handleElementRename(@NotNull String newElementName) {
        return super.handleElementRename(newElementName);
    }

    public @NotNull Object[] getVariants() {
        return new Object[0];
    }

    private Collection<VirtualFile> getVirtualFiles(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            logger.error("Project base path is null");
        }

        String targetPath = basePath + "/src/main/resources/mybatis";
        VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (directory == null || !directory.isDirectory()) {
            logger.error("Target directory does not exist or is not a directory: " + targetPath);
        }

        GlobalSearchScope scope = new GlobalSearchScopesCore.DirectoryScope(project, directory, true);
        FileType xmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("xml");
        return FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, xmlFileType, scope);
    }
}
