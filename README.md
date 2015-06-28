Rep Template System
===================

The *Rep Template System* is a free and open source library to create programs
that dynamically generate HTML pages based on template files.

The primary goal of any web template system is separate the HTML code of the
pages of a web site from the data that needs to be displayed in those pages.
The HTML code is then stored in template files, separated from the data, and
both will be used to generate the final web pages. This separation makes
web sites easier to maintain, because the web design becomes more independent
of the internal programming, and vice versa.

The Rep Template System has the following features:

- Valid HTML in templates, hiding the template information in HTML comments.
- Easy and simple template language that cannot include executable instructions.
- Complete pages in templates, not separated in multiple template files.
- Pages generated progressively in a stream, consuming less memory.
- Enforced parallel structure of the template and the programs that use it.

Examples
--------

### Variables ###

A Rep template can include *variables* that will be automatically replaced
with values at the moment of generating the final page, for example, this
is a template with variables:

    <html>
    <head><!--rep var=pagetitle place="My Page"-->
    <title>My Page</title>
    </head>
    <body><!--rep var=pageintro place="Explanation of the page."-->
    <h1>My Page</h1>
    <p>Explanation of the page.</p>
    </body>
    </html>

A variable is defined creating a special HTML comment that includes a name
and a string to be replaced, both declared in the `var` and `place` attributes
of the comment. If you save that HTML in a file then you can write a program
to generate web pages based on that template. For example, the following Java
program reads the template from a file named `listpage.tpl.html`:

    String file = "listpage.tpl.html";
    RepBlk tpl = new RepBlk(new BufferedReader(new FileReader(file)));
    tpl.setVar("pagetitle", "LIST OF WINNERS 2015");
    tpl.setVar("pageintro", "THE TOP PERFORMERS OF THE YEAR!");
    tpl.start(new BufferedWriter(new OutputStreamWriter(System.out)));
    tpl.end();

The object `RepBlk` is the initial block and represents the template, and
the call to `start` begins the writing of the page in the selected destination,
replacing the variables found in the text with its values. This program prints
the following page:

    <html>
    <head>
    <title>LIST OF WINNERS 2015</title>
    </head>
    <body>
    <h1>LIST OF WINNERS 2015</h1>
    <p>THE TOP PERFORMERS OF THE YEAR!</p>
    </body>
    </html>

In the previous example program, the values of the variables are included
in the code, but usually the program obtains them from a different place,
like an external file or database, specially when it needs to generate
many different pages using the same template and different data.

The template now can be modified in many ways without having to change the
program, for example by modifying the HTML code or the position and number of
occurrences of the variables. The page can be generated again with the same
program and it will have the changes made to the template.

### Blocks ###

A Rep template can have marked sections of the HTML, called *blocks*, that the
program can repeat zero or more times. A block can contain other blocks inside,
and each one of them can contain others, forming a hierarchy of blocks.
Each block is identified by a name, so two blocks inside the same parent block
cannot have the same name. Here is the previous template with an added block:

    <html>
    <head><!--rep var=pagetitle place="My Page"-->
    <title>My Page</title>
    </head>
    <body><!--rep var=pageintro place="Explanation of the page."-->
    <h1>My Page</h1>
    <p>Explanation of the page.</p>
    <ol><!--rep blk=item--><!--rep var=name place="Name"-->
    <!--rep var=num place="100"--><li>Name: 100 points</li><!--/rep-->
    </ol>
    </body>
    </html>

The block named `item` is delimited between the two special HTML comments
`<!--rep blk=item-->` and `<!--/rep-->`, and contains two variables,
`name` and `num`. The program can repeat any block many times in
the generated page, and those will be written together in the place of
the page where that block appears in the template. The following
program writes the page repeating the block `item` three times with
different values for its variables:

    String file = "listpage.tpl.html";
    RepBlk tpl = new RepBlk(new BufferedReader(new FileReader(file)));
    RepBlk item = tpl.getBlk("item");
    tpl.setVar("pagetitle", "LIST OF WINNERS 2015");
    tpl.setVar("pageintro", "THE TOP PERFORMERS OF THE YEAR!");
    tpl.start(new BufferedWriter(new OutputStreamWriter(System.out)));
    item.setVar("name", "Smith");
    item.setVar("num", "500");
    item.start();
    item.setVar("name", "Johnson");
    item.setVar("num", "450");
    item.start();
    item.setVar("name", "Jackson");
    item.setVar("num", "400");
    item.start();
    tpl.next();
    tpl.end();

By calling to `getBlk` the program can access to a block that is inside
another. In this example, the block named `item` is obtained from the initial
block that represents the template. After calling to `start` on a block, the
program must call to `start` on the first block that is inside that one.
A block can be repeated by calling to `start` the desired number of times,
but it can only be repeated in the position designed by the template, or
the call will throw an exception. If a block must be repeated zero times,
then `skip` must be called instead of `start`.

When a block is not going to be repeated again (or when it has been skipped),
the program must call to `next` on its parent block, to write the text after
that child block in the template. In the example, this text is the HTML after
the closing comment of the block `item`, that is, the last lines having
`</ol></body></html>` that are part of the initial block. If a block contains
two or more blocks inside, `next` must be called on the parent block after each
child block is repeated (or skipped), to write the text between children blocks
and also the text after the last child block.

The call to `end` is optional, but ensures that the page has been completely
written or it will throw an exception. The generated page for the given
program using the given template is:

    <html>
    <head>
    <title>LIST OF WINNERS 2015</title>
    </head>
    <body>
    <h1>LIST OF WINNERS 2015</h1>
    <p>THE TOP PERFORMERS OF THE YEAR!</p>
    <ol>
    <li>Smith: 500 points</li>
    <li>Johnson: 450 points</li>
    <li>Jackson: 400 points</li>
    </ol>
    </body>
    </html>

Note that in a real program, the data for each block repetition should be
obtained from an external source, and some form of iteration will be used to
write any number of blocks (also calling to `skip` when that number is zero).

### Order ###

The following diagram shows graphically the correct order of calls for any block
with other blocks inside:

                                  .--.
         .-----------------------(skip)-----------------------.
        /             _____       '--' _____                   \
       /  .---.      |     |   .--.   |     |   .--.            \
    --'--(start)--.--|BLOCK|--(next)--|BLOCK|--(next)-- ... --.--'--
          '---'  /   |_____|   '--'   |_____|   '--'           \
                 \                     .---.                   /
                  '-------------------(start)-----------------'
                                       '...'

The `RepBlk` objects will enforce this order to write all the parts of the
page in the exact order defined by the template, making possible, at the
same time, the progressive generation of the page.

Note that this feature creates an essential restriction of the Rep Template
System that affects to the separation between templates and programs: The
blocks in a template cannot be reordered without having to change all the
programs that use that template. However, this task should be easy since the
structure of the programs must be equal to the structure of the blocks in
the template. A good design centralizing the code that uses a certain template
can minimize the impact of this problem.

Use the developer documentation written in comments in the code to read
a more complete description of all the methods described here, and others.

----------------

For any questions, comments or suggestions, please contact me:

Carlos Rica Espinosa<br>
jasampler@gmail.com
