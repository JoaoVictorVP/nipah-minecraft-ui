package nipah.minecraft.ui.ksp

class KGen {
    interface Maker {
        fun make(): String
    }
    class Source(val packageName: String, val fileName: String): Maker {
        private val imports = mutableListOf<String>()
        private val makers = mutableListOf<Maker>()

        infix fun import(import: String): Source {
            imports.add(import)
            return this
        }

        infix fun maker(maker: Maker): Source {
            makers.add(maker)
            return this
        }

        override fun make(): String {
            return (if(packageName.isNotEmpty()) "package $packageName\n\n" else "\n") +
                    imports.joinToString("\n") { "import $it" } + "\n\n" +
                    makers.joinToString("\n\n") { it.make() }
        }
    }

    class Singleton(val name: String): Maker {
        private val inherits = mutableListOf<Class.Inherits>()
        private var init: String? = null
        private val fields = mutableListOf<Field>()
        private val funs = mutableListOf<Fun>()
        private var raw: String? = null

        infix fun inherits(inherits: Class.Inherits): Singleton {
            this.inherits.add(inherits)
            return this
        }

        infix fun raw(raw: String): Singleton {
            this.raw = raw
            return this
        }

        infix fun init(init: String): Singleton {
            this.init = init
            return this
        }

        infix fun with(field: Field): Singleton {
            fields.add(field)
            return this
        }

        infix fun with(`fun`: Fun): Singleton {
            funs.add(`fun`)
            return this
        }

        override fun make(): String {
            if(raw != null) {
                return "object $name${if(inherits.isNotEmpty()) ": " + inherits.joinToString(", ") { it.make() } else ""} {\n" +
                        "$raw\n}"
            }

            return "object $name${if(inherits.isNotEmpty()) ": " + inherits.joinToString(", ") { it.make() } else ""} {\n" +
                    "${fields.joinToString("\n") { it.make() }}\n" +
                    "${if(init == null) "" else "init {\n$init\n}"}\n" +
                    "${funs.joinToString("\n") { it.make() }}\n}"
        }
    }

    class Interface(private val name: String): Maker {
        private val attributes = mutableListOf<String>()
        private val inherits = mutableListOf<Class.Inherits>()
        private val fields: MutableList<Field> = mutableListOf()
        private val funs = mutableListOf<Fun>()

        private var raw: String? = null

        infix fun attribute(attribute: String): Interface {
            attributes.add(attribute)
            return this
        }

        infix fun inherits(inherits: Class.Inherits): Interface {
            this.inherits.add(inherits)
            return this
        }

        infix fun raw(raw: String): Interface {
            this.raw = raw
            return this
        }

        infix fun with(field: Field): Interface {
            fields.add(field)
            return this
        }

        infix fun with(`fun`: Fun): Interface {
            funs.add(`fun`)
            return this
        }

        override fun make(): String {
            if(raw != null) {
                return "${if(attributes.isNotEmpty()) "${attributes.joinToString("\n") { "@$it" }}\n" else ""}interface $name: ${inherits.joinToString(", ") { it.make() }} {\n" +
                        "$raw\n}"
            }

            return "${if(attributes.isNotEmpty()) "${attributes.joinToString("\n") { "@$it" }}\n" else ""}interface $name: ${inherits.joinToString(", ") { it.make() }} {\n" +
                    "${fields.joinToString("\n") { it.make() }}\n" +
                    "${funs.joinToString("\n") { it.make() }}\n}"
        }
    }

    class Class(private val name: String): Maker {
        private val attributes = mutableListOf<String>()
        private val inherits = mutableListOf<Inherits>()
        private var companionObject: CompanionObject? = null
        private var primaryCtor: PrimaryConstructor? = null
        private var init: String? = null
        private var ctors = mutableListOf<Constructor>()
        private val fields = mutableListOf<Field>()
        private val funs = mutableListOf<Fun>()
        private var isAbstract = false
        private var raw: String? = null

        infix fun attribute(attribute: String): Class {
            attributes.add(attribute)
            return this
        }

        infix fun inherits(inherits: Inherits): Class {
            this.inherits.add(inherits)
            return this
        }

        infix fun raw(raw: String): Class {
            this.raw = raw
            return this
        }

        infix fun with(companionObject: CompanionObject): Class {
            this.companionObject = companionObject
            return this
        }

        infix fun with(ctor: PrimaryConstructor): Class {
            primaryCtor = ctor
            return this
        }

        infix fun init(init: String): Class {
            this.init = init
            return this
        }

        infix fun with(ctor: Constructor): Class {
            ctors.add(ctor)
            return this
        }

        infix fun with(field: Field): Class {
            fields.add(field)
            return this
        }

        infix fun with(`fun`: Fun): Class {
            funs.add(`fun`)
            return this
        }

        infix fun abstract(abstract: Boolean): Class {
            isAbstract = abstract
            return this
        }

        override fun make(): String {
            if(raw != null) {
                return "${if(attributes.isNotEmpty()) "${attributes.joinToString("\n") { "@$it" }}\n" else ""}${if(isAbstract) "abstract " else ""}class $name${primaryCtor?.make() ?: ""}${if(inherits.isNotEmpty()) ": " + inherits.joinToString(", ") { it.make() } else ""} {\n" +
                        "$raw\n}"
            }

            return "${if(attributes.isNotEmpty()) "${attributes.joinToString("\n") { "@$it" }}\n" else ""}${if(isAbstract) "abstract " else ""}class $name${primaryCtor?.make() ?: ""}${if(inherits.isNotEmpty()) ": " + inherits.joinToString(", ") { it.make() } else ""} {\n" +
                    (if(companionObject != null) "${companionObject!!.make()}\n" else "") +
                    "${fields.joinToString("\n") { it.make() }}\n" +
                    "${ctors.joinToString("\n") { it.make() }}\n" +
                    "${if(init == null) "" else "init {\n$init\n}"}\n" +
                    "${funs.joinToString("\n") { it.make() }}\n}"
        }

        class CompanionObject: Maker {
            private val fields = mutableListOf<Field>()
            private val funs = mutableListOf<Fun>()

            infix fun with(field: Field): CompanionObject {
                fields.add(field)
                return this
            }

            infix fun with(`fun`: Fun): CompanionObject {
                funs.add(`fun`)
                return this
            }

            override fun make(): String {
                return "companion object {\n${fields.joinToString("\n") { it.make() }}\n" +
                        "${funs.joinToString("\n") { it.make() }}\n}"
            }
        }

        class Inherits(private val type: String, private val ctor: String? = null): Maker {
            private var withGenerics: String? = null

            infix fun generics(generics: String): Inherits {
                withGenerics = generics
                return this
            }

            override fun make(): String {
                return "$type${if(withGenerics == null) "" else "<$withGenerics>"}${if(ctor == null) "" else "($ctor)"}"
            }
        }
    }
    class PrimaryConstructor(private val params: MutableList<Param> = mutableListOf()): Maker {
        infix fun with(param: Param): PrimaryConstructor {
            params.add(param)
            return this
        }

        override fun make(): String {
            return "(${params.joinToString(", ") { it.make() }})"
        }
    }
    class Constructor(private val params: MutableList<Param> = mutableListOf()): Maker {
        private var upperCalling: String? = null
        private var body: String? = null

        infix fun with(param: Param): Constructor {
            params.add(param)
            return this
        }

        infix fun calling(upperCalling: String): Constructor {
            this.upperCalling = upperCalling
            return this
        }

        infix fun body(body: String): Constructor {
            this.body = body
            return this
        }

        override fun make(): String {
            return "constructor(${params.joinToString(", ") { it.make() }})${if(upperCalling != null) ": $upperCalling" else ""}" +
                    if(body != null) "{\n${body ?: ""}\n}" else ""
        }
    }
    class Fun(private val name: String, private val params: MutableList<Param> = mutableListOf(), private var body: String = ""): Maker {
        companion object {
            fun override(name: String, params: MutableList<Param> = mutableListOf(), body: String = ""): Fun {
                return Fun(name, params, body).apply {
                    override = true
                }
            }
        }

        private var override = false
        private var returnType: String? = null

        infix fun with(param: Param): Fun {
            params.add(param)
            return this
        }

        infix fun returns(type: String): Fun {
            returnType = type
            return this
        }

        infix fun body(body: String): Fun {
            this.body = body
            return this
        }

        override fun make(): String {
            return "${if(override) "override " else ""}fun $name(${params.joinToString(", ") { it.make() }})${if(returnType != null) ": $returnType" else ""} {\n$body\n}"
        }
    }
    class Param(private val name: String, private val type: String, private val defaultValue: String? = null): Maker {
        override fun make(): String {
            return "$name: $type" + if (defaultValue != null) " = $defaultValue" else ""
        }
    }
    class Field(private val name: String, private val type: String, private val defaultValue: String? = null): Maker {
        private var isPrivate = false
        private var isLateInit = false
        private var isVar = false

        fun private(private: Boolean = true): Field {
            isPrivate = private
            return this
        }

        fun lateInit(lateInit: Boolean = true): Field {
            isLateInit = lateInit
            isVar = true
            return this
        }

        fun asVar(`var`: Boolean = true): Field {
            isVar = `var`
            return this
        }

        override fun make(): String {
            val privateModifier = if(isPrivate) "private " else ""
            val lateInitModifier = if(isLateInit) "lateinit " else ""
            val valModifier = if(isVar) "var" else "val"

            if(type == "") {
                if(defaultValue.isNullOrEmpty()) throw Exception("Field $name has no type and no default value")
                return "$privateModifier$lateInitModifier$valModifier $name = $defaultValue"
            }

            return "$privateModifier$lateInitModifier$valModifier $name: $type" + if (defaultValue != null) " = $defaultValue" else ""
        }
    }
}

fun String.firstLetterAsCapital(): String {
    return this.replaceFirstChar { it.uppercase() }
}
