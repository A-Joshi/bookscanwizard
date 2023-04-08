Notes:

I don\'t have a user guide, but this has some notes to help you get
started.

**Starting BSW:**

**Preview**

Using preview will preview the process for the indicated up to where the
cursor is in the configuration. So if you want it to run the whole
configuration, move the cursor to the end of the configuration. If you
are adjusting a step, move the cursor to just before the step. Run Batch

**Using the viewer:**

The viewer can be used to preview the results of operations, and to
select coordinates for commands like Crop and Perspective.

To select coordinates, click on the points in the viewer. You will see
draw a box around the points you select. After you have selected the
coordinates, either right click and choose an operation, or copy the
selection and past it to the configuration.

You can also copy coordinates from the configuration to the viewer.
Either right click in the configuration, and choose the option to
transfer the coordinates, or copy and paste the coordinates into the
viewer.

**Running from the command line:**

To run a batch from the command line, add -batch to the command line.
This will run the batch instead of bringing up the interactive tool.

**If you aren\'t getting the results expected:**

If you aren\'t getting what you are expecting, verify that the text
cursor is in the right spot. As you add each command, verify you are
getting the right behavior. Also pay attention to the Pages commands.
Pages will define what the operations should be preformed on until
another Pages command is executed.

For example if you have:

    Pages: IMG_234

    Crop: 100, 100, 1000, 1000

    Pages: all

    Crop: 200, 200, 1000, 1000

This will crop IMG_0000, then the next step will recrop page IMG_0000 as
well as all other documents. If you want to crop all pages except that
page you have to do something like:

    Pages: -IMG_233,IMG_235-

Also note that the commands are case-sensitive, so if you run:

pages: all

This will not work.
