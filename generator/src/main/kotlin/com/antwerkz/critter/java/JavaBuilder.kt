package com.antwerkz.critter.java

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.TypeSafeFieldEnd
import com.antwerkz.critter.criteria.BaseCriteria
import com.antwerkz.critter.nameCase
import com.mongodb.WriteConcern
import com.mongodb.WriteResult
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Reference
import org.mongodb.morphia.query.Criteria
import org.mongodb.morphia.query.FieldEndImpl
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.QueryImpl
import org.mongodb.morphia.query.UpdateOperations
import org.mongodb.morphia.query.UpdateResults
import java.io.File
import java.io.PrintWriter
import org.jboss.forge.roaster.model.Visibility.PACKAGE_PRIVATE as rPACKAGE_PRIVATE
import org.jboss.forge.roaster.model.Visibility.PRIVATE as rPRIVATE
import org.jboss.forge.roaster.model.Visibility.PROTECTED as rPROTECTED
import org.jboss.forge.roaster.model.Visibility.PUBLIC as rPUBLIC

class JavaBuilder(val context: CritterContext) {

    fun build(directory: File) {
        context.classes.values.forEach { source ->
            val criteriaClass = Roaster.create(JavaClassSource::class.java)
                    .setPackage(source.pkgName + ".criteria")
                    .setName(source.name + "Criteria")

            val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
            if (context.shouldGenerate(source.lastModified(), outputFile.lastModified())) {
                extractFields(source as JavaClass, criteriaClass)

                if (!source.hasAnnotation(Embedded::class.java)) {
                    criteriaClass.superType = """${BaseCriteria::class.java.name}<${source.qualifiedName}>"""
                    criteriaClass.addMethod()
                            .setConstructor(true)
                            .setPublic()
                            .setBody("super(ds, ${source.name}.class);")
                            .addParameter(Datastore::class.java, "ds")
                } else {
                    criteriaClass.addField()
                            .setType(Query::class.java)
                            .setName("query")
                            .setPrivate()
                    criteriaClass.addField()
                            .setType("String")
                            .setName("prefix")
                            .setPrivate()

                    criteriaClass.addMethod().apply {
                        isConstructor = true
                        setPublic()
                        body = """this.query = query;
    this.prefix = prefix + ".";"""
                        addParameter(Query::class.java, "query")
                        addParameter(String::class.java, "prefix")
                    }
                }

                buildUpdater(source, criteriaClass)

                generate(outputFile, criteriaClass)
            }

        }
    }

    private fun extractFields(source: JavaClass, criteriaClass: JavaClassSource) {
        source.fields.forEach { field ->
            criteriaClass.addField()
                    .setName(field.name)
                    .setPublic()
                    .setStatic(true)
                    .setFinal(true)
                    .setStringInitializer(field.mappedName())
                    .setType(String::class.java)

            addField(source, criteriaClass, field)
        }
/*
        source.superClass?.let {
            extractFields(it as JavaClass, criteriaClass)
        }
*/
    }

    private fun buildUpdater(source: CritterClass, criteriaClass: JavaClassSource) {
        criteriaClass.addImport(Query::class.java)
        criteriaClass.addImport(UpdateOperations::class.java)
        criteriaClass.addImport(UpdateResults::class.java)
        criteriaClass.addImport(WriteConcern::class.java)
        criteriaClass.addImport(WriteResult::class.java)

        val type = source.name + "Updater"
        criteriaClass.addMethod().apply {
            setPublic()
            name = "getUpdater"
            setReturnType(type)
            body = "return new $type();"
        }

        val updater: JavaClassSource = criteriaClass.addNestedType(JavaClassSource::class.java)
        updater.name = type
        updater.addField().apply {
            name = "updateOperations"
            setType("UpdateOperations<${source.name}>")
            setLiteralInitializer("ds.createUpdateOperations(${source.name}.class);")
        }

        updater.addMethod().apply {
            setPublic()
            name = "updateAll"
            setReturnType(UpdateResults::class.java)
            body = "return ds.update(query(), updateOperations, false);"
        }

        updater.addMethod().apply {
            setPublic()
            name = "updateFirst"
            setReturnType(UpdateResults::class.java)
            body = "return ds.updateFirst(query(), updateOperations, false);"
        }

        updater.addMethod().apply {
            addParameter(WriteConcern::class.java.simpleName, "wc")

            setPublic()
            name = "updateAll"
            setReturnType(UpdateResults::class.java)
            body = "return ds.update(query(), updateOperations, false, wc);"
        }

        updater.addMethod().apply {
            addParameter(WriteConcern::class.java.simpleName, "wc")

            setPublic()
            name = "updateFirst"
            setReturnType(UpdateResults::class.java)
            body = "return ds.updateFirst(query(), updateOperations, false, wc);"
        }

        updater.addMethod().apply {
            setPublic()
            name = "upsert"
            setReturnType(UpdateResults::class.java)
            body = "return ds.update(query(), updateOperations, true);"
        }

        updater.addMethod().apply {
            addParameter(WriteConcern::class.java.simpleName, "wc")

            setPublic()
            name = "upsert"
            setReturnType(UpdateResults::class.java)
            body = "return ds.update(query(), updateOperations, true, wc);"
        }

        updater.addMethod().apply {
            setPublic()
            name = "remove"
            setReturnType(WriteResult::class.java.simpleName)
            body = "return ds.delete(query());"
        }

        updater.addMethod().apply {
            addParameter(WriteConcern::class.java.simpleName, "wc")

            setPublic()
            name = "remove"
            setReturnType(WriteResult::class.java)
            body = "return ds.delete(query(), wc);"
        }

        source.fields
                .filter({ field -> !field.isStatic })
                .forEach { field ->
                    field.parameterTypes
                            .forEach { criteriaClass.addImport(it) }

                    criteriaClass.addImport(field.type)
                    if (!field.hasAnnotation(Id::class.java)) {
                        updater.addMethod().apply {
                            addParameter(field.parameterizedType, "value")

                            name = field.name
                            body = "updateOperations.set(\"${field.name}\", value);\nreturn this;"
                            setPublic()
                            setReturnType(type)
                        }

                        updater.addMethod().apply {
                            name = "unset${field.name.nameCase()}"
                            body = "updateOperations.unset(\"${field.name}\");\nreturn this;"
                            setPublic()
                            setReturnType(type)
                        }

                        numerics(type, updater, field)
                        containers(type, updater, field)
                    }
                }
    }

    private fun numerics(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isNumeric()) {
            updater.addMethod().apply {
                name = "dec${field.name.nameCase()}"
                body = """updateOperations.dec("${field.name}");
return this;"""
                setPublic()
                setReturnType(type)
            }

            updater.addMethod().apply {
                name = "dec${field.name.nameCase()}"
                body = """updateOperations.dec("${field.name}", value);
return this;"""
                setPublic()
                setReturnType(type)
                addParameter(field.type, "value")
            }

            updater.addMethod().apply {
                name = "inc${field.name.nameCase()}"
                body = """updateOperations.inc("${field.name}");
return this;"""
                setPublic()
                setReturnType(type)
            }

            updater.addMethod().apply {
                name = "inc${field.name.nameCase()}"
                body = "updateOperations.inc(\"${field.name}\", value);\nreturn this;"
                setPublic()
                setReturnType(type)
                addParameter(field.type, "value")
            }
        }
    }

    private fun containers(type: String, updater: JavaClassSource, field: CritterField) {
        if (field.isContainer()) {

            updater.addMethod().apply {
                name = "addTo${field.name.nameCase()}"
                body = "updateOperations.add(\"${field.name}\", value);\nreturn this;"

                setPublic()
                addParameter(field.parameterizedType, "value")
                setReturnType(type)
            }

            updater.addMethod().apply {
                name = "addTo${field.name.nameCase()}"
                body = """updateOperations.add("${field.name}", value, addDups);
return this;"""

                setPublic()
                setReturnType(type)
                addParameter(field.parameterizedType, "value")
                addParameter("boolean", "addDups")
            }

            updater.addMethod().apply {
                name = "addAllTo${field.name.nameCase()}"
                body = """updateOperations.addAll("${field.name}", values, addDups);
return this;"""
                setPublic()
                setReturnType(type)
                addParameter(field.parameterizedType, "values")
                addParameter("boolean", "addDups")
            }

            updater.addMethod().apply {
                name = "removeFirstFrom${field.name.nameCase()}"
                body = "updateOperations.removeFirst(\"${field.name}\");\nreturn this;"
                setPublic()
                setReturnType(type)
            }

            updater.addMethod().apply {
                name = "removeLastFrom${field.name.nameCase()}"
                body = "updateOperations.removeLast(\"${field.name}\");\nreturn this;"
                setPublic()
                setReturnType(type)
            }

            updater.addMethod().apply {
                name = "removeFrom${field.name.nameCase()}"
                body = "updateOperations.removeAll(\"${field.name}\", value);\nreturn this;"

                setPublic()
                setReturnType(type)
                addParameter(field.parameterizedType, "value")
            }

            updater.addMethod().apply {
                name = "removeAllFrom${field.name.nameCase()}"
                body = "updateOperations.removeAll(\"${field.name}\", values);\nreturn this;"

                setPublic()
                setReturnType(type)
                addParameter(field.parameterizedType, "values")
            }
        }
    }

    private fun addField(source: CritterClass, criteriaClass: JavaClassSource, field: CritterField) {
        if (source.hasAnnotation(Reference::class.java)) {
            criteriaClass.addMethod().apply {
                name = source.name
                body = """query.filter("${source.name} = ", reference);
        return this;"""
                setPublic()
                setReturnType(criteriaClass.qualifiedName)
                addParameter(field.type, "reference")
            }
        } else if (field.hasAnnotation(Embedded::class.java)) {
            criteriaClass.addImport(Criteria::class.java)
            val criteriaType: String
            if (!field.shortParameterTypes.isEmpty()) {
                criteriaType = field.shortParameterTypes[0] + "Criteria"
                criteriaClass.addImport("${criteriaClass.`package`}.$criteriaType")
            } else {
                criteriaType = field.type + "Criteria"
                criteriaClass.addImport(field.type)
            }
            criteriaClass.addMethod().apply {
                name = field.name
                body = """return new $criteriaType(query, "${source.name}");"""
                setPublic()
                setReturnType(criteriaType)
            }
        } else if (!field.isStatic) {
//            val qualifiedName = field.fullyQualifiedType
//            criteriaClass.addImport(qualifiedName)
            criteriaClass.addImport(field.type)
            criteriaClass.addImport(Criteria::class.java)
            criteriaClass.addImport(FieldEndImpl::class.java)
            criteriaClass.addImport(QueryImpl::class.java)
            var fieldName = """"${field.name}""""
            if (field.hasAnnotation(Embedded::class.java) || context.isEmbedded(name = field.type)) {
                fieldName = "prefix + " + fieldName
            }
            criteriaClass.addMethod().apply {
                name = field.name
                body = """return new TypeSafeFieldEnd<>(this, query, $fieldName);"""
                setPublic()
                setReturnType("${TypeSafeFieldEnd::class.java.name}<${criteriaClass.qualifiedName}, ${field.type}>")
            }
            criteriaClass.addMethod().apply {
                name = field.name
                body = "return new TypeSafeFieldEnd<>(this, query, $fieldName).equal(value);"
                setPublic()
                setReturnType(Criteria::class.java)
                addParameter(field.type, "value")
            }
        }
    }

    private fun generate(outputFile: File, criteriaClass: JavaClassSource) {
        outputFile.parentFile.mkdirs()
        PrintWriter(outputFile).use { writer -> writer.println(criteriaClass.toString()) }
    }
}
