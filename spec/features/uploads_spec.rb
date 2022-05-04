require "features/shared/authentication_error"

describe "Uploads", type: :feature do
  let(:path) { "/media-service/uploads/" }
  let(:file) { File.read("spec/support/files/small.txt") }

  context "for public access" do
    it "displays disabled file input" do
      visit path

      expect(page).to have_field(nil, type: :file, disabled: true)
    end

    it "displays modal with authentication error" do
      visit path
      within(".modal") do
        expect(page).to have_css(".modal-header", text: "Request ERROR 403")
        expect(page).to have_css(".modal-body", text: "Sign-in required")
      end
    end

    it_displays "authentication error"
  end

  context "for signed in user" do
    let(:user) { create(:user) }
    let(:chunk_size) { 100 }
    let!(:settings) { create(:media_service_setting, upload_min_part_size: chunk_size) }
    let!(:store) { create(:media_store, :database, :with_users, users: [user]) }

    before do
      sign_in
      visit path
    end

    it "allows to upload a file" do
      expect(true).to be

      within "#uploads-page .form" do
        attach_file nil, "spec/support/files/small.txt"
      end


      expect(page).to have_css(".modal-body",
                               text: /POST \/media-service\/uploads\/[a-z0-9-]+\/complete/)
      expect(page).to have_link("Original", href: /\/media-service\/originals\/[a-z0-9-]+$/)

      click_link "Original"
      expect(current_path).to match(/^\/media-service\/originals\/[a-z0-9-]+$/)
    end
  end
end
