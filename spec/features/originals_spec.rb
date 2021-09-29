require "features/shared/authentication_error"

shared_examples "media file details" do
  it "displays View link" do
    expect(page)
      .to have_link("View",
                    href: /\/media-service\/originals\/#{media_file.id}\/content\?download=false$/)

    click_link "View"
    expect(page.body).to include(file_content)
  end

  it "displays Download link" do
    expect(page)
      .to have_link("Download",
                    href: /\/media-service\/originals\/#{media_file.id}\/content\?download=true$/)

    expect(File).not_to exist(BROWSER_DOWNLOAD_DIR + "/small.txt")

    click_link "Download"

    expect(File).to exist(BROWSER_DOWNLOAD_DIR + "/small.txt")
    expect(File.read(BROWSER_DOWNLOAD_DIR + "/small.txt")).to eq(file_content)
  end

  it "displays filename" do
    expect(page).to have_content("Filename\n#{media_file.filename}")
  end

  it "displays file size" do
    expect(page).to have_content "Size\n132.0B"
  end
end

describe "Originals", type: :feature do
  let(:path) { "/media-service/originals/#{media_file.id}" }
  let(:file_path) { "spec/support/files/small.txt" }
  let(:file_content) { File.read(file_path) }

  before do
    FileUtils.rm_f(BROWSER_DOWNLOAD_DIR + "/small.txt")
  end

  context "for public acccess" do
    let(:media_file) { create(:media_file_for_image) }

    it_displays "authentication error"
  end

  context "for signed in user" do
    let(:user) { create(:user) }
    let(:media_file) { OpenStruct.new(id: upload_file_and_return_id) }

    before do
      sign_in
      visit path
    end

    it_displays "media file details"
  end
end

def upload_file_and_return_id
  create(:media_service_setting, upload_min_part_size: 100)
  create(:media_store, :database, :with_users, users: [user])

  visit "/media-service/uploads/"

  within "#uploads-page .form" do
    attach_file nil, file_path
  end

  expect(page).to have_css(".modal-body",
                            text: /POST \/media-service\/uploads\/[a-z0-9-]+\/complete/)
  link_to_original = find_link("Original")
  link_to_original[:href]
    .match(/\/media-service\/originals\/([a-z0-9-]+)$/)
    .captures
    .first
end
